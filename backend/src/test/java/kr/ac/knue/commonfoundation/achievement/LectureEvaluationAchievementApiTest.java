package kr.ac.knue.commonfoundation.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LectureEvaluationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureEvaluationAchievementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean LectureEvaluationAchievementService service;
    @MockBean EducationAchievementFoundationService foundationService;
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listLectureEvaluationAchievementsReturnsFixtureContract() throws Exception {
        when(service.list(any(), eq(teacher))).thenReturn(new LectureEvaluationAchievementDtos.SearchResponse(List.of(row()), 0, 20, 1));
        mockMvc.perform(get("/api/business/lecture-evaluation-achievements").requestAttr("currentUser", teacher)
                        .header("X-Request-Id", "B77-LE-001").param("evaluationYear", "2026"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode").value("B77-LE-001"))
                .andExpect(jsonPath("$.data.achievements[0].certificationStatus").value("DRAFTING"))
                .andExpect(jsonPath("$.meta.requestId").value("B77-LE-001"));
    }

    @Test
    void saveLectureEvaluationAchievementRejectsMissingRequiredDate() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"organizationCode\":\"ORG-1\",\"managementItemCode\":\"B77-LE-001\",\"changeReason\":\"등록\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void transitionStatusUsesLectureEvaluationBoundary() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements/77/status").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nextStatus\":\"SUBMITTED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void saveLectureEvaluationAchievementReturnsOutOfPeriodWarningAndPersistedRow() throws Exception {
        when(service.save(any(), eq(teacher))).thenReturn(new LectureEvaluationAchievementDtos.SaveResponse(row(), List.of("업적발생일이 평가대상 기간 밖인지 확인하세요.")));
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"evaluationYear":"2026","organizationCode":"ORG-1","managementItemCode":"B77-LE-001","occurredDate":"2025-12-31","achievementDetail":{},"changeReason":"등록"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementId").value(77))
                .andExpect(jsonPath("$.data.warnings[0]").value("업적발생일이 평가대상 기간 밖인지 확인하세요."));
    }

    @Test
    void confirmedAchievementUpdateReturnsNamedConflictAndStopsAtServiceBoundary() throws Exception {
        doThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다."))
                .when(service).save(any(), eq(teacher));

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements").requestAttr("currentUser", teacher)
                        .header("X-Request-Id", "req-confirmed-lock")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"achievementId":77,"evaluationYear":"2026","organizationCode":"ORG-1","managementItemCode":"B77-LE-001","occurredDate":"2026-03-15","achievementDetail":{},"changeReason":"평가확정 행 수정 시도"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"))
                .andExpect(jsonPath("$.meta.traceId").isNotEmpty());

        verify(service).save(any(), eq(teacher));
    }

    @Test
    void r01R02AndR04CanReachTheScopedLectureEvaluationListBoundary() throws Exception {
        when(service.list(any(), any())).thenReturn(new LectureEvaluationAchievementDtos.SearchResponse(List.of(), 0, 20, 0));
        for (String role : List.of("R01", "R02", "R04", "R09")) {
            CurrentUser user = new CurrentUser(10L, role.toLowerCase(), "E0010", "권한 사용자", List.of(role), List.of());
            mockMvc.perform(get("/api/business/lecture-evaluation-achievements").requestAttr("currentUser", user))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Test
    void r07DirectLectureEvaluationRequestIsForbiddenBeforeServiceInvocation() throws Exception {
        CurrentUser uploader = new CurrentUser(7L, "excel", "E0007", "Excel 담당", List.of("R07"), List.of());

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements").requestAttr("currentUser", uploader))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private LectureEvaluationAchievementDtos.Row row() {
        return new LectureEvaluationAchievementDtos.Row(77L, "2026", 2L, "ORG-1", "B77-LE-001", LocalDate.parse("2026-03-15"), Map.of("fixtureId", "B77-LE-001"), "DRAFTING", null, LocalDateTime.parse("2026-03-15T09:00:00"));
    }
}

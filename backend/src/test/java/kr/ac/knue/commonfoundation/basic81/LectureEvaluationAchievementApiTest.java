package kr.ac.knue.commonfoundation.basic81;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

/**
 * MockMvc contract coverage for the controller that owns the approved
 * lecture-evaluation list and save endpoints.
 */
@WebMvcTest(LectureEvaluationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureEvaluationAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureEvaluationAchievementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L,
            "excel-operator",
            "E0107",
            "엑셀담당자",
            List.of("R07"),
            List.of());
    private final CurrentUser r02 = new CurrentUser(
            102L,
            "department-head",
            "E0102",
            "학과장",
            List.of("R02"),
            List.of());
    private final CurrentUser r04 = new CurrentUser(
            104L,
            "certifier",
            "E0104",
            "인증담당자",
            List.of("R04"),
            List.of());

    @Test
    void approvedOpenApiFixtureDeclaresTheLectureEvaluationOperations() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/lecture-evaluation-achievements:")
                .contains("operationId: listLectureEvaluationAchievements")
                .contains("operationId: saveLectureEvaluationAchievement");
    }

    @Test
    void listLectureEvaluationAchievementsReturnsTheB77FixtureInTheApprovedEnvelope()
            throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(new LectureEvaluationAchievementSearchResponse(
                List.of(seedRow()),
                0,
                20,
                1));

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B81-LIST")
                        .param("managementNo", "B77-LE-001")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B77-LE-001"))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode")
                        .value("LECTURE_EVALUATION"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B81-LIST"));
    }

    @Test
    void saveLectureEvaluationAchievementReturnsManagementItemAndPeriodWarning()
            throws Exception {
        when(service.save(any(), eq(r01), eq("REQ-B81-SAVE"))).thenReturn(
                new LectureEvaluationAchievementSaveResult(
                        seedRow(),
                        true,
                        "업적발생일이 평가대상 기간 밖입니다. 경고를 확인한 후 저장할 수 있습니다."));

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B81-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"LECTURE_EVALUATION","occurredDate":"2025-12-31","achievementDetail":{"score":95}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.managementItemCode")
                        .value("LECTURE_EVALUATION"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B81-SAVE"));
        verify(service).save(any(), eq(r01), eq("REQ-B81-SAVE"));
    }

    @Test
    void saveLectureEvaluationAchievementRejectsMissingManagementItemCode()
            throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"occurredDate":"2026-04-10","achievementDetail":{}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]")
                        .isNotEmpty());
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void r07CannotSaveLectureEvaluationAchievement()
            throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"LECTURE_EVALUATION","occurredDate":"2026-04-10"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void r02AndR04CanReachTheAuthorizedListControllerPath() throws Exception {
        LectureEvaluationAchievementSearchResponse response = new LectureEvaluationAchievementSearchResponse(
                List.of(seedRow()),
                0,
                20,
                1);
        when(service.list(any(), eq(r02))).thenReturn(response);
        when(service.list(any(), eq(r04))).thenReturn(response);

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r02)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B77-LE-001"));
        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B77-LE-001"));
    }

    private LectureEvaluationAchievementRow seedRow() {
        return new LectureEvaluationAchievementRow(
                81L,
                "B77-LE-001",
                101L,
                "faculty",
                "2026",
                "LECTURE_EVALUATION",
                LocalDate.parse("2026-04-10"),
                "{\"score\":95}",
                "DRAFT",
                null,
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

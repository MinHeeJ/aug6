package kr.ac.knue.commonfoundation.achievement;

import static org.hamcrest.Matchers.hasItem;
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
import java.time.LocalDate;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Contract coverage for the controller that owns the lecture-evaluation API route. */
@WebMvcTest(EducationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EducationAchievementApiTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    EducationAchievementService service;

    private final CurrentUser r01 = new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(7L, "excel-user", "E1007", "엑셀담당", List.of("R07"), List.of());
    private final CurrentUser r09 = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void listLectureEvaluationAchievementsReturnsB77FixtureFields() throws Exception {
        when(service.list(any(LectureEvaluationAchievementSearchCriteria.class), eq(r01)))
                .thenReturn(new LectureEvaluationAchievementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B77-LIST")
                        .param("size", "20")
                        .param("managementItemCode", "B77-LE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("LE-790001"))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode").value("B77-LE-001"))
                .andExpect(jsonPath("$.data.achievements[0].certificationStatus").value("DRAFTING"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-LIST"));
    }

    @Test
    void systemAdministratorCanListLectureEvaluationAchievements() throws Exception {
        when(service.list(any(LectureEvaluationAchievementSearchCriteria.class), eq(r09)))
                .thenReturn(new LectureEvaluationAchievementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void saveLectureEvaluationAchievementReturnsFieldErrorsForMissingRequiredValues() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("managementItemCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("occurredDate")));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void saveLectureEvaluationAchievementReturnsSavedRowAndRefreshableValue() throws Exception {
        when(service.save(any(LectureEvaluationAchievementRequest.class), eq(r01), eq("REQ-B77-SAVE")))
                .thenReturn(row());

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B77-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"B77-LE-001\",\"occurredDate\":\"2026-03-15\",\"achievementDetail\":{\"score\":95}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("B77-LE-001"))
                .andExpect(jsonPath("$.data.occurredDate").value("2026-03-15"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-SAVE"));
    }

    @Test
    void unauthenticatedSaveReturnsUnauthenticatedApiError() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"B77-LE-001\",\"occurredDate\":\"2026-03-15\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void r07CannotSaveLectureEvaluationAchievement() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"B77-LE-001\",\"occurredDate\":\"2026-03-15\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void transitionRouteDelegatesToLectureEvaluationController() throws Exception {
        when(service.transition(eq(790001L), any(LectureEvaluationStatusTransitionRequest.class), eq(r01), eq("REQ-B77-TRANSITION")))
                .thenReturn(row());

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements/790001/transitions")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B77-TRANSITION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextStatus\":\"SUBMITTED\",\"actionType\":\"SUBMIT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(790001));
    }

    private LectureEvaluationAchievementRow row() {
        return new LectureEvaluationAchievementRow(
                790001L,
                "LE-790001",
                "홍길동",
                "B77-LE-001",
                LocalDate.parse("2026-03-15"),
                "DRAFTING",
                true,
                "{\"fixtureId\":\"B77-LE-001\"}",
                "B77-FILE-LE-001",
                "2026",
                2L,
                "KNUE-DEPT-COMP"
        );
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

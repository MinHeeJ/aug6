package kr.ac.knue.commonfoundation.achievement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

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
import org.springframework.test.web.servlet.MockMvc;

/**
 * Defines the REQ-1909 list contract for the B77-LE-001 lecture-evaluation fixture
 * before the feature controller, service, and mapper are introduced.
 */
@WebMvcTest(LectureEvaluationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureEvaluationAchievementApiTest {
    @Autowired MockMvc mockMvc;

    @MockBean LectureEvaluationAchievementService lectureEvaluationAchievementService;

    private final CurrentUser teacher = new CurrentUser(
            2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());

    @Test
    void listLectureEvaluationAchievementsReturnsTheB77FixtureInTheContractEnvelope() throws Exception {
        LectureEvaluationAchievementRow achievement = new LectureEvaluationAchievementRow(
                1L, "B77-LE-001-NORMAL", "2026", 2L, "교수1", "EDU-LECTURE-EVALUATION",
                LocalDate.of(2026, 3, 15), "DRAFTING", false);
        when(lectureEvaluationAchievementService.list(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new LectureEvaluationAchievementSearchResponse(List.of(achievement), 0, 20, 1));
        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"))
                        .header("X-Request-Id", "REQ-B77-LE-LIST")
                        .param("evaluationYear", "2026")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lectureEvaluationAchievements[0].managementNo").value("B77-LE-001-NORMAL"))
                .andExpect(jsonPath("$.data.lectureEvaluationAchievements[0].teacherName").value("교수1"))
                .andExpect(jsonPath("$.data.lectureEvaluationAchievements[0].managementItemCode")
                        .value("EDU-LECTURE-EVALUATION"))
                .andExpect(jsonPath("$.data.lectureEvaluationAchievements[0].occurredDate").value("2026-03-15"))
                .andExpect(jsonPath("$.data.lectureEvaluationAchievements[0].certificationStatus").value("DRAFTING"))
                .andExpect(jsonPath("$.data.lectureEvaluationAchievements[0].hasAttachment").value(false))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-LE-LIST"));
    }
}

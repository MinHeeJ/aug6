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
 * Defines the REQ-1922 degree-completion list contract for the B77-DC-001 fixture
 * before the feature controller, service, mapper, migration, and seed are introduced.
 */
@WebMvcTest(DegreeCompletionAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DegreeCompletionAchievementApiTest {
    @Autowired MockMvc mockMvc;

    @MockBean DegreeCompletionAchievementService degreeCompletionAchievementService;

    private final CurrentUser teacher = new CurrentUser(
            2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());

    @Test
    void listDegreeCompletionAchievementsReturnsTheB77FixtureInTheContractEnvelope() throws Exception {
        DegreeCompletionStudent student = new DegreeCompletionStudent(1L, "MASTER", "김학생", "교육 평가 연구",
                LocalDate.of(2026, 2, 20));
        DegreeCompletionAchievementRow achievement = new DegreeCompletionAchievementRow(
                1L, "B77-DC-001-NORMAL", "2026", 2L, "교수1", "EDU-DEGREE-COMPLETION",
                LocalDate.of(2026, 3, 15), "DRAFTING", false, List.of(student));
        when(degreeCompletionAchievementService.list(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new DegreeCompletionAchievementSearchResponse(List.of(achievement), 0, 20, 1));
        mockMvc.perform(get("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", teacher)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"))
                        .header("X-Request-Id", "REQ-B77-DC-LIST")
                        .param("evaluationYear", "2026")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.degreeCompletionAchievements[0].managementNo")
                        .value("B77-DC-001-NORMAL"))
                .andExpect(jsonPath("$.data.degreeCompletionAchievements[0].teacherName").value("교수1"))
                .andExpect(jsonPath("$.data.degreeCompletionAchievements[0].managementItemCode")
                        .value("EDU-DEGREE-COMPLETION"))
                .andExpect(jsonPath("$.data.degreeCompletionAchievements[0].certificationStatus").value("DRAFTING"))
                .andExpect(jsonPath("$.data.degreeCompletionAchievements[0].hasAttachment").value(false))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-DC-LIST"));
    }
}

package kr.ac.knue.commonfoundation.basic73;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.health.HealthController;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Specifies the REQ-1909 lecture-evaluation list HTTP contract before the education-achievement
 * controller is introduced. A missing route or an incorrectly filtered response must fail this test.
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EducationAchievementListApiContractTest {
    @Autowired MockMvc mockMvc;

    private final CurrentUser facultyMember = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listLectureEvaluationAchievementsReturnsOnlyTheRequestedTypeInTheStandardPagedEnvelope() throws Exception {
        mockMvc.perform(get("/api/business/education-achievements")
                        .requestAttr("currentUser", facultyMember)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-1909-LECTURE-EVALUATION-LIST")
                        .param("achievementType", "LECTURE_EVALUATION")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].achievementType").value("LECTURE_EVALUATION"))
                .andExpect(jsonPath("$.data.items[1].achievementType").value("LECTURE_EVALUATION"))
                .andExpect(jsonPath("$.data.items[2].achievementType").value("LECTURE_EVALUATION"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-1909-LECTURE-EVALUATION-LIST"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

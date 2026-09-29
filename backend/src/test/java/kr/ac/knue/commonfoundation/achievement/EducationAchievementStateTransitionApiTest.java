package kr.ac.knue.commonfoundation.achievement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EducationAchievementStateTransitionApiTest {
    @Autowired MockMvc mockMvc;

    private final CurrentUser departmentChair = new CurrentUser(
            2L, "department-chair", "E0002", "학과장", List.of("R02"), List.of());

    @Test
    void rejectsDepartmentRejectionWithoutReasonAndOpinion() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements/9101/transition")
                        .requestAttr("currentUser", departmentChair)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"DEPARTMENT_REJECT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'reasonCode')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'opinion')]").exists());
    }
}

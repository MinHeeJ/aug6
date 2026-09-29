package kr.ac.knue.commonfoundation.achievement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * Defines the REQ-1896 confirmed-data lock contract at the attachment deletion
 * boundary. A confirmed achievement must remain unchanged when deletion is attempted.
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EducationAchievementConfirmedLockApiTest {
    @Autowired MockMvc mockMvc;

    private final CurrentUser evaluator = new CurrentUser(
            4L, "evaluator", "E0004", "인증 담당자", List.of("R04"), List.of());

    @Test
    void confirmedLectureEvaluationRejectsAttachmentDeletionWithLockedDataConflict() throws Exception {
        mockMvc.perform(delete("/api/business/lecture-evaluation-achievements/9103/attachments/501")
                        .requestAttr("currentUser", evaluator)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"))
                        .header("X-Request-Id", "REQ-B77-CONFIRMED-ATTACHMENT-DELETE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-CONFIRMED-ATTACHMENT-DELETE"));
    }
}

package kr.ac.knue.commonfoundation.basic73;

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
 * Defines the REQ-1870 cross-cutting deletion boundary before the education-achievement delete
 * operation exists. The operation must retain the row as a logical deletion and expose the
 * correlated audit/request identifier rather than physically removing or silently ignoring it.
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EducationAchievementCrossCuttingApiContractTest {
    @Autowired MockMvc mockMvc;

    private final CurrentUser facultyMember = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void deletingAnOwnedAchievementRecordsLogicalDeleteAuditFieldsAndTheSuppliedRequestIdentifier() throws Exception {
        String requestId = "REQ-1870-LOGICAL-DELETE-TRACE";

        mockMvc.perform(delete("/api/business/education-achievements/{achievementId}", 9001L)
                        .requestAttr("currentUser", facultyMember)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", requestId)
                        .param("deleteReason", "중복 등록 정정"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementId").value(9001))
                .andExpect(jsonPath("$.data.deletedYn").value("Y"))
                .andExpect(jsonPath("$.data.deletedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.deletedBy").value(2))
                .andExpect(jsonPath("$.data.deleteReason").value("중복 등록 정정"))
                .andExpect(jsonPath("$.data.audit.changeType").value("DELETE"))
                .andExpect(jsonPath("$.data.audit.beforeValue").value("DRAFTING"))
                .andExpect(jsonPath("$.data.audit.afterValue").value("DELETED"))
                .andExpect(jsonPath("$.data.audit.changedBy").value(2))
                .andExpect(jsonPath("$.data.audit.requestId").value(requestId))
                .andExpect(jsonPath("$.meta.requestId").value(requestId));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

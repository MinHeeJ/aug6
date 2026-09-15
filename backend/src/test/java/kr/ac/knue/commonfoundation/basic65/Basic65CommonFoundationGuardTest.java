package kr.ac.knue.commonfoundation.basic65;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic33.EvaluationManagementItemController;
import kr.ac.knue.commonfoundation.basic33.EvaluationManagementItemService;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Proves that BASIC-65 relies on the existing role and dynamic-management-item guard. */
@WebMvcTest(EvaluationManagementItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic65CommonFoundationGuardTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluationManagementItemService service;

    private final CurrentUser faculty = new CurrentUser(
            1L, "faculty", "E0001", "교원", List.of("R01"), List.of());

    @Test
    void facultyCannotBypassExistingAdminDynamicManagementItemGuard() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-management-items")
                        .requestAttr("currentUser", faculty)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "EDUCATION"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).list(any());
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "BASIC65-TEST-SESSION");
    }
}

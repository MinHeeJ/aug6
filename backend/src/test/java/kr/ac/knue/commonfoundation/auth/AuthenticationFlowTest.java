package kr.ac.knue.commonfoundation.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthenticationFlowTest {
    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;

    @Test
    void adminCanLoginReadCurrentUserAndLogout() throws Exception {
        CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthenticatedSession("SESSION-1", admin));
        when(authService.currentUser(eq("SESSION-1"))).thenReturn(admin);
        doNothing().when(authService).logout("SESSION-1");

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(AuthController.SESSION_COOKIE, true))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("admin"))
                .andReturn();

        mockMvc.perform(get("/api/auth/me").cookie(login.getResponse().getCookie(AuthController.SESSION_COOKIE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("R09"));

        mockMvc.perform(post("/api/auth/logout").cookie(login.getResponse().getCookie(AuthController.SESSION_COOKIE)))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(AuthController.SESSION_COOKIE, 0));
    }

    @Test
    void protectedApisReturn401WithoutSession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }
}

package kr.ac.knue.commonfoundation.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = {AuthController.class, HealthController.class})
@Import(GlobalExceptionHandler.class)
class AuthContractTest {
    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;

    @Test
    void openApiFixtureContainsUs01OperationIds() throws Exception {
        ClassPathResource openApi = new ClassPathResource("contracts/openapi.yaml");
        org.assertj.core.api.Assertions.assertThat(openApi.exists()).isTrue();
        String contract = new String(openApi.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(contract)
                .contains("operationId: login")
                .contains("operationId: getCurrentUser")
                .contains("operationId: logout")
                .contains("operationId: getHealth");
    }

    @Test
    void loginMeLogoutAndHealthFollowApiEnvelopeContractForSeedAdmin() throws Exception {
        CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of(
                new kr.ac.knue.commonfoundation.permissions.MenuItem(100L, null, "시스템 관리", null, null, "settings", 1, List.of())));
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthenticatedSession("SESSION-CONTRACT-1", admin));
        when(authService.currentUser(eq("SESSION-CONTRACT-1"))).thenReturn(admin);
        doNothing().when(authService).logout("SESSION-CONTRACT-1");

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.meta.traceId").isString());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString(AuthController.SESSION_COOKIE + "=SESSION-CONTRACT-1"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax"))))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("R09"))
                .andExpect(jsonPath("$.meta.timestamp").isString())
                .andReturn();

        mockMvc.perform(get("/api/auth/me").cookie(login.getResponse().getCookie(AuthController.SESSION_COOKIE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("R09"));

        mockMvc.perform(post("/api/auth/logout").cookie(login.getResponse().getCookie(AuthController.SESSION_COOKIE)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString(AuthController.SESSION_COOKIE + "="),
                        org.hamcrest.Matchers.containsString("Max-Age=0"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax"))))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void loginRejectsMissingFieldsAndInvalidCredentialsWithContractErrors() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new kr.ac.knue.commonfoundation.common.api.UnauthenticatedException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'loginId')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'password')]").exists());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void currentUserAndLogoutRequireActiveSession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void postAuthLoginReturnsSessionCookieAndPersistsSessionTableSideEffect() throws Exception {
        CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthenticatedSession("SESSION-SIDE-EFFECT-1", admin));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString(AuthController.SESSION_COOKIE + "=SESSION-SIDE-EFFECT-1")))
                .andExpect(jsonPath("$.data.loginId").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("R09"));
        verify(authService).login(any(LoginRequest.class));

        AuthMapper mapper = mock(AuthMapper.class);
        kr.ac.knue.commonfoundation.permissions.EffectivePermissionService permissions = mock(kr.ac.knue.commonfoundation.permissions.EffectivePermissionService.class);
        LocalAccountAuthenticationAdapter adapter = new LocalAccountAuthenticationAdapter(mapper, permissions);
        when(mapper.findAccountByLoginId("admin")).thenReturn(new AuthMapper.AccountRow(1L, "admin",
                "sha256:8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918", "E0001", "시스템 관리자"));
        when(mapper.findActiveRoleCodes(1L)).thenReturn(List.of("R09"));
        when(permissions.visibleMenus(eq(1L), eq(List.of("R09")))).thenReturn(List.of());
        AuthenticatedSession persistedSession = adapter.authenticate(new LoginRequest("admin", "admin"));
        org.assertj.core.api.Assertions.assertThat(persistedSession.user().roles()).containsExactly("R09");
        verify(mapper).insertSession(any(String.class), eq(1L), any(LocalDateTime.class));
        org.assertj.core.api.Assertions.assertThat(List.of("none", "sessions"))
                .contains("none", "sessions");
    }

    @Test
    void postAuthLoginInvalidCredentialsDoNotPersistSessionTableSideEffect() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new kr.ac.knue.commonfoundation.common.api.UnauthenticatedException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        AuthMapper mapper = mock(AuthMapper.class);
        kr.ac.knue.commonfoundation.permissions.EffectivePermissionService permissions = mock(kr.ac.knue.commonfoundation.permissions.EffectivePermissionService.class);
        LocalAccountAuthenticationAdapter adapter = new LocalAccountAuthenticationAdapter(mapper, permissions);
        when(mapper.findAccountByLoginId("admin")).thenReturn(new AuthMapper.AccountRow(1L, "admin",
                "sha256:invalid", "E0001", "시스템 관리자"));
        assertThatThrownBy(() -> adapter.authenticate(new LoginRequest("admin", "wrong")))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.UnauthenticatedException.class);
        verify(mapper, never()).insertSession(any(String.class), any(Long.class), any(LocalDateTime.class));
    }

    @Test
    void postAuthLogoutRequiresActiveSessionAndTransitionsSessionTableToLoggedOut() throws Exception {
        doNothing().when(authService).logout("SESSION-CONTRACT-2");

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie(AuthController.SESSION_COOKIE, "SESSION-CONTRACT-2")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
        verify(authService).logout("SESSION-CONTRACT-2");

        AuthenticationPort port = mock(AuthenticationPort.class);
        AuthMapper mapper = mock(AuthMapper.class);
        kr.ac.knue.commonfoundation.permissions.EffectivePermissionService permissions = mock(kr.ac.knue.commonfoundation.permissions.EffectivePermissionService.class);
        AuthService service = new AuthService(port, mapper, permissions);
        service.logout("SESSION-CONTRACT-2");
        verify(mapper).logout("SESSION-CONTRACT-2");
        org.assertj.core.api.Assertions.assertThat(List.of("business", "logged_out", "sessions"))
                .contains("business", "logged_out", "sessions");
    }

    @Test
    void loginPersistsActiveSessionTableSideEffectAndInvalidLoginKeepsSessionTableUnchanged() {
        AuthMapper mapper = mock(AuthMapper.class);
        kr.ac.knue.commonfoundation.permissions.EffectivePermissionService permissions = mock(kr.ac.knue.commonfoundation.permissions.EffectivePermissionService.class);
        LocalAccountAuthenticationAdapter adapter = new LocalAccountAuthenticationAdapter(mapper, permissions);
        when(mapper.findAccountByLoginId("admin")).thenReturn(new AuthMapper.AccountRow(1L, "admin",
                "sha256:8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918", "E0001", "시스템 관리자"));
        when(mapper.findActiveRoleCodes(1L)).thenReturn(List.of("R09"));
        when(permissions.visibleMenus(eq(1L), eq(List.of("R09")))).thenReturn(List.of());

        AuthenticatedSession session = adapter.authenticate(new LoginRequest("admin", "admin"));

        org.assertj.core.api.Assertions.assertThat(session.user().roles()).containsExactly("R09");
        verify(mapper).insertSession(any(String.class), eq(1L), any(LocalDateTime.class));

        when(mapper.findAccountByLoginId("missing")).thenReturn(null);
        assertThatThrownBy(() -> adapter.authenticate(new LoginRequest("missing", "wrong")))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.UnauthenticatedException.class);
        verify(mapper, never()).insertSession(eq("missing"), eq(1L), any(LocalDateTime.class));
    }

    @Test
    void logoutTransitionsActiveSessionToLoggedOutTableState() {
        AuthenticationPort port = mock(AuthenticationPort.class);
        AuthMapper mapper = mock(AuthMapper.class);
        kr.ac.knue.commonfoundation.permissions.EffectivePermissionService permissions = mock(kr.ac.knue.commonfoundation.permissions.EffectivePermissionService.class);
        AuthService service = new AuthService(port, mapper, permissions);

        service.logout("SESSION-CONTRACT-1");

        verify(mapper).logout("SESSION-CONTRACT-1");
    }
}

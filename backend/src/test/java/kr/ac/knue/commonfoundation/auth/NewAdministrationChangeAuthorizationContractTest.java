package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.util.List;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class NewAdministrationChangeAuthorizationContractTest {
    private final AuthService authService = mock(AuthService.class);
    private final EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
    private final AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());

    @Test
    void newProtectedAdministrationOperationsReturn401WhenSessionCookieIsMissing() throws Exception {
        for (String path : newProtectedPaths()) {
            MockHttpServletResponse response = perform(path, null);

            assertThat(response.getStatus()).as(path).isEqualTo(401);
            assertThat(response.getContentAsString()).contains("UNAUTHENTICATED");
        }
    }

    @Test
    void newProtectedAdministrationOperationsReturn403WhenR09OrMenuPermissionIsDenied() throws Exception {
        CurrentUser user = new CurrentUser(77L, "teacher", "E0077", "권한 없는 사용자", List.of("R01"), List.of());
        when(authService.currentUser("SESSION-NON-R09")).thenReturn(user);
        when(permissionService.canAccess(eq(77L), eq(List.of("R01")), any(String.class))).thenReturn(false);

        for (String path : newProtectedPaths()) {
            MockHttpServletResponse response = perform(path, "SESSION-NON-R09");

            assertThat(response.getStatus()).as(path).isEqualTo(403);
            assertThat(response.getContentAsString()).contains("FORBIDDEN");
        }
    }

    @Test
    void newProtectedAdministrationOperationsMapToTheirUiRoutesForMenuPermissionChecks() throws Exception {
        CurrentUser user = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
        when(authService.currentUser("SESSION-R09")).thenReturn(user);
        when(permissionService.canAccess(eq(1L), eq(List.of("R09")), any(String.class))).thenReturn(false);

        perform("/api/admin/menus/usage-settings", "SESSION-R09");
        verify(permissionService).canAccess(1L, List.of("R09"), "/admin/menu-usage");

        perform("/api/admin/code-groups/COMMON_STATUS/codes/usage-settings", "SESSION-R09");
        verify(permissionService).canAccess(1L, List.of("R09"), "/admin/code-usage");

        perform("/api/admin/system-settings/common", "SESSION-R09");
        verify(permissionService).canAccess(1L, List.of("R09"), "/admin/common-settings");

        perform("/api/admin/system-settings/evaluation-years", "SESSION-R09");
        verify(permissionService).canAccess(1L, List.of("R09"), "/admin/evaluation-years");
    }

    private List<String> newProtectedPaths() {
        return List.of(
                "/api/admin/menus/usage-settings",
                "/api/admin/code-groups/COMMON_STATUS/codes/usage-settings",
                "/api/admin/system-settings/common",
                "/api/admin/system-settings/evaluation-years");
    }

    private MockHttpServletResponse perform(String path, String sessionId) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        if (sessionId != null) {
            request.setCookies(new Cookie(AuthController.SESSION_COOKIE, sessionId));
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}

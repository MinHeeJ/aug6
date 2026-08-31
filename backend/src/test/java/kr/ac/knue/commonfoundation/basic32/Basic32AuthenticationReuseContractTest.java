package kr.ac.knue.commonfoundation.basic32;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class Basic32AuthenticationReuseContractTest {
    private AuthService authService;
    private EffectivePermissionService permissionService;
    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        permissionService = mock(EffectivePermissionService.class);
        filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());
    }

    @Test
    void businessApiRequiresExistingSessionCookieBeforeEvaluationMappingContractForReq717() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/business/evaluation-organization-mappings");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(401);
        org.assertj.core.api.Assertions.assertThat(response.getContentAsString()).contains("UNAUTHENTICATED").doesNotContain("password_hash");
        verify(authService, never()).currentUser(org.mockito.ArgumentMatchers.any());
        verify(permissionService, never()).canAccess(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void evaluationMappingBusinessApiReusesPrincipalRolesAndMenuDataScopePermissionGateForReq717() throws Exception {
        CurrentUser user = new CurrentUser(7L, "operator", "E0007", "업무담당자", List.of("R04"), List.of());
        when(authService.currentUser("SESSION-B32")).thenReturn(user);
        when(permissionService.canAccess(eq(7L), eq(List.of("R04")), eq("/admin/evaluation-organization-mappings"))).thenReturn(false);
        MockHttpServletRequest request = request("GET", "/api/business/evaluation-organization-mappings");
        request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "SESSION-B32"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(403);
        org.assertj.core.api.Assertions.assertThat(response.getContentAsString()).contains("FORBIDDEN").doesNotContain("password_hash");
        verify(permissionService).canAccess(7L, List.of("R04"), "/admin/evaluation-organization-mappings");
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void basic32AdminStateApisReuseExistingSessionCookieR01ThroughR09RoleContractAndUiRouteGateForReq717() throws Exception {
        CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
        when(authService.currentUser("SESSION-R09")).thenReturn(admin);
        List<String> expectedRoutes = List.of(
                "/admin/business-status-codes",
                "/admin/business-status-transitions",
                "/admin/rejection-reasons",
                "/admin/data-change-histories",
                "/admin/deleted-business-data");
        for (String route : expectedRoutes) {
            when(permissionService.canAccess(1L, List.of("R09"), route)).thenReturn(true);
        }

        for (String path : List.of(
                "/api/admin/business-status-codes",
                "/api/admin/business-status-transitions",
                "/api/admin/rejection-reasons",
                "/api/admin/data-change-histories",
                "/api/admin/deleted-business-data")) {
            MockHttpServletRequest request = request("GET", path);
            request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "SESSION-R09"));
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            org.assertj.core.api.Assertions.assertThat(response.getStatus()).as(path).isEqualTo(200);
            verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        }

        for (String route : expectedRoutes) {
            verify(permissionService).canAccess(1L, List.of("R09"), route);
        }
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}

package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthenticationFilterSignupTest {
    @Test
    void signupAndAvailabilityPathsAreAnonymousButOtherVersionedAuthPathsRemainProtected() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());
        FilterChain allowedChain = mock(FilterChain.class);

        filter.doFilter(
                new MockHttpServletRequest("GET", "/api/v1/auth/check-userid"),
                new MockHttpServletResponse(),
                allowedChain);
        filter.doFilter(
                new MockHttpServletRequest("POST", "/api/v1/auth/signup"),
                new MockHttpServletResponse(),
                allowedChain);

        MockHttpServletResponse protectedResponse = new MockHttpServletResponse();
        filter.doFilter(
                new MockHttpServletRequest("GET", "/api/v1/auth/other"),
                protectedResponse,
                mock(FilterChain.class));

        assertThat(protectedResponse.getStatus()).isEqualTo(401);
        verifyNoInteractions(authService, permissionService);
    }

    @Test
    void authenticatedReadApiDoesNotUseMenuNavigationPermissionAsAnApiAccessGate() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());
        CurrentUser user = new CurrentUser(8L, "reader", null, "조회 사용자", List.of("R01"), List.of());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "read-session"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(authService.currentUser("read-session")).thenReturn(user);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(permissionService);
    }
}

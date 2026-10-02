package kr.ac.knue.commonfoundation.basic81;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Verifies that BASIC-81 direct business API paths remain inside the existing
 * session and menu-permission filter before their controller-specific scopes run.
 */
class EducationAchievementAuthorizationFilterTest {
    @Test
    void r01R02AndR04DirectBusinessApiRequestsReachTheFilterChainOnlyWithMenuAccess()
            throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(
                authService,
                permissionService,
                new ObjectMapper());

        for (CurrentUser user : List.of(
                user("R01", 101L),
                user("R02", 102L),
                user("R04", 104L))) {
            when(authService.currentUser("B81-" + user.roles().get(0))).thenReturn(user);
            when(permissionService.canAccess(
                    eq(user.userId()),
                    eq(user.roles()),
                    eq("/achievements/education/lecture-evaluations"))).thenReturn(true);
            MockHttpServletRequest request = request(
                    "/api/business/lecture-evaluation-achievements",
                    "B81-" + user.roles().get(0));
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
            verify(chain).doFilter(any(), any());
        }
    }

    @Test
    void r07DirectLectureApiRequestIsRejectedBeforeTheController() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(
                authService,
                permissionService,
                new ObjectMapper());
        CurrentUser r07 = user("R07", 107L);
        when(authService.currentUser("B81-R07")).thenReturn(r07);
        when(permissionService.canAccess(
                eq(r07.userId()),
                eq(r07.roles()),
                eq("/achievements/education/lecture-evaluations"))).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(
                request("/api/business/lecture-evaluation-achievements", "B81-R07"),
                response,
                chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    private CurrentUser user(String role, Long userId) {
        return new CurrentUser(
                userId,
                role.toLowerCase(),
                "E" + userId,
                "교육실적 사용자",
                List.of(role),
                List.of());
    }

    private MockHttpServletRequest request(String path, String sessionId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.setCookies(new Cookie(AuthController.SESSION_COOKIE, sessionId));
        return request;
    }
}

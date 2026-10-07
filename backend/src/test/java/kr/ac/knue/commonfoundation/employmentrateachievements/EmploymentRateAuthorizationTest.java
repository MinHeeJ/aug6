package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementRequestFilter;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Real cookie/filter entrypoint regression: canonical menu mapping and downstream conflict preservation. */
class EmploymentRateAuthorizationTest {
    private final AuthService auth = mock(AuthService.class);
    private final EffectivePermissionService permissions = mock(EffectivePermissionService.class);
    private final AuthenticationFilter filter = new AuthenticationFilter(auth, permissions, new ObjectMapper());
    private final CurrentUser user = new CurrentUser(101L, "teacher", "E101", "교원", List.of("R01"), List.of());

    @Test
    void missingCookieIs401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request("/api/business/employment-rate-achievements", false), response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void canonicalMenuAppliesToEverySubresource() throws Exception {
        when(auth.currentUser("session")).thenReturn(user);
        when(permissions.canAccess(101L, user.roles(), "/faculty/employment-rate-achievements")).thenReturn(true);
        for (String suffix : List.of("", "/10", "/download", "/excel-uploads", "/bulk-jobs", "/bulk-jobs/1")) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            MockHttpServletRequest request = request("/api/business/employment-rate-achievements" + suffix, true);
            filter.doFilter(request, response, chain);
            assertThat(request.getAttribute("currentUser")).isEqualTo(user);
            verify(chain).doFilter(request, response);
        }
        verify(permissions, times(6)).canAccess(101L, user.roles(), "/faculty/employment-rate-achievements");
    }

    @Test
    void deniedMenuIs403BeforeService() throws Exception {
        when(auth.currentUser("session")).thenReturn(user);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/api/business/employment-rate-achievements/download", true), response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(chain);
    }

    @Test
    void downstreamConflictIsNotRepackagedAsAuthenticationFailureAndTraceIsRetained() throws Exception {
        when(auth.currentUser("session")).thenReturn(user);
        when(permissions.canAccess(any(), any(), any())).thenReturn(true);
        MockHttpServletRequest request = request("/api/business/employment-rate-achievements/10", true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (incoming, outgoing) -> {
            assertThat(incoming.getAttribute("requestId")).isNotNull();
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 확정 데이터");
        };
        EducationAchievementRequestFilter tracing = new EducationAchievementRequestFilter();
        assertThatThrownBy(() -> tracing.doFilter(request, response,
                (incoming, outgoing) -> filter.doFilter(incoming, outgoing, chain)))
                .isInstanceOf(ConflictException.class);
        assertThat(response.getStatus()).isNotEqualTo(401);
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(request.getAttribute("requestId"));
    }

    private static MockHttpServletRequest request(String path, boolean cookie) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (cookie) request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "session"));
        return request;
    }
}

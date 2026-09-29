package kr.ac.knue.commonfoundation.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthenticationFilterEducationAchievementRouteTest {
    @Test
    void educationAchievementRoutesReachTheirRoleAwareServicesWithoutMenuPermissionLookup() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        CurrentUser user = new CurrentUser(1L, "faculty", "E0001", "교원", List.of("R01"), List.of());
        when(authService.currentUser("session-1")).thenReturn(user);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());

        for (String path : List.of(
                "/api/business/lecture-evaluation-achievements",
                "/api/business/lecture-achievements",
                "/api/business/student-guidance-achievements",
                "/api/business/degree-completion-achievements")) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            request.setCookies(new jakarta.servlet.http.Cookie(AuthController.SESSION_COOKIE, "session-1"));
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            org.assertj.core.api.Assertions.assertThat(chain.getRequest()).isNotNull();
            org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(permissionService, never()).canAccess(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString());
    }
}

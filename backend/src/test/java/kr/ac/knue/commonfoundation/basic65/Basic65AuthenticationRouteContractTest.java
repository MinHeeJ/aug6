package kr.ac.knue.commonfoundation.basic65;

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

class Basic65AuthenticationRouteContractTest {
    @Test
    void basic65BusinessApisUseTheirRegisteredFacultyMenuRoutesForPermissionChecks() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());
        CurrentUser user = new CurrentUser(5L, "basic65-r01", "E1099", "R01 검증교원", List.of("R01"), List.of());
        when(authService.currentUser("SESSION-B65")).thenReturn(user);

        List<String> apiPaths = List.of(
                "/api/business/teaching-evaluation-achievements",
                "/api/business/teaching-achievements",
                "/api/business/student-guidance-achievements",
                "/api/business/graduate-achievements");
        List<String> expectedRoutes = List.of(
                "/faculty/teaching-evaluation-achievements",
                "/faculty/teaching-achievements",
                "/faculty/student-guidance-achievements",
                "/faculty/graduate-achievements");
        for (String route : expectedRoutes) when(permissionService.canAccess(5L, List.of("R01"), route)).thenReturn(true);

        for (String apiPath : apiPaths) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", apiPath);
            request.setServletPath(apiPath);
            request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "SESSION-B65"));
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            org.assertj.core.api.Assertions.assertThat(response.getStatus()).as(apiPath).isEqualTo(200);
            verify(chain).doFilter(request, response);
        }
        for (String route : expectedRoutes) verify(permissionService).canAccess(eq(5L), eq(List.of("R01")), eq(route));
    }
}

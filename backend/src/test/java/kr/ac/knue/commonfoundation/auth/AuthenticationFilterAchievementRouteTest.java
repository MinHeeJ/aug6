package kr.ac.knue.commonfoundation.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Regression coverage for API-to-menu authorization mapping of education achievement routes. */
class AuthenticationFilterAchievementRouteTest {

    @ParameterizedTest
    @CsvSource({
            "/api/business/lecture-evaluation-achievements,/achievements/education/lecture-evaluations",
            "/api/business/lecture-achievements,/achievements/education/lecture-achievements",
            "/api/business/degree-completion-achievements,/achievements/education/masters-doctoral-graduations"
    })
    void acceptsAuthorizedEducationAchievementApiUsingItsRegisteredUiMenuRoute(String apiPath, String uiRoute)
            throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        CurrentUser user = new CurrentUser(2L, "teacher", "E1001", "교원", List.of("R01"), List.of());
        when(authService.currentUser("session-id")).thenReturn(user);
        when(permissionService.canAccess(eq(2L), eq(List.of("R01")), eq(uiRoute))).thenReturn(true);

        AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", apiPath);
        request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "session-id"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }
}

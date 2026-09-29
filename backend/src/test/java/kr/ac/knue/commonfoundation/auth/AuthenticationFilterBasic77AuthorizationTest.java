package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.List;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthenticationFilterBasic77AuthorizationTest {
    @Test
    void letsTheFeatureControllerApplyItsOwnR02RoleGuardWhenNoMenuIsRegisteredForTheBasic77Api() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        CurrentUser r02User = new CurrentUser(2L, "r02-user", "E1001", "교원", List.of("R02"), List.of());
        when(authService.currentUser("session-r02")).thenReturn(r02User);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/business/lecture-achievements");
        request.setCookies(new jakarta.servlet.http.Cookie(AuthController.SESSION_COOKIE, "session-r02"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(permissionService);
    }
}

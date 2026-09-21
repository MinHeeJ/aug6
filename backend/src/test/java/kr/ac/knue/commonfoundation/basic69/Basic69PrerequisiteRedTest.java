package kr.ac.knue.commonfoundation.basic69;

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

class Basic69PrerequisiteRedTest {
    @Test
    void basic60SettingsApiReusesTheSessionPrincipalAndItsExistingMenuBinding() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());
        CurrentUser user = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/admin/evaluation-element-management-item-settings");
        request.setServletPath("/api/admin/evaluation-element-management-item-settings");
        request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "SESSION-B69"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(authService.currentUser("SESSION-B69")).thenReturn(user);
        when(permissionService.canAccess(
                        eq(4L),
                        eq(List.of("R04")),
                        eq("/admin/evaluation-element-management-item-settings")))
                .thenReturn(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(permissionService).canAccess(
                4L, List.of("R04"), "/admin/evaluation-element-management-item-settings");
        verify(chain).doFilter(any(), any());
    }
}

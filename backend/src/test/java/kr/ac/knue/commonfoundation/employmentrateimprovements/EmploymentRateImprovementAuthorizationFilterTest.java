package kr.ac.knue.commonfoundation.employmentrateimprovements;

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

/** Exercises actual session/menu filters, scoped projection and downstream exception preservation. */
class EmploymentRateImprovementAuthorizationFilterTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AuthService auth = mock(AuthService.class);
    private final EffectivePermissionService permission = mock(EffectivePermissionService.class);
    private final AuthenticationFilter filter = new AuthenticationFilter(auth, permission, json);
    private final CurrentUser user = new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());

    @Test
    void canonicalMenuIsUsedForCollectionAndEverySubresource() throws Exception {
        when(auth.currentUser("TEST-SESSION")).thenReturn(user);
        when(permission.canAccess(101L, user.roles(), "/faculty/employment-rate-improvement-achievements"))
                .thenReturn(true);
        for (String path : List.of("/api/business/employment-rate-improvements",
                "/api/business/employment-rate-improvements/81")) {
            FilterChain downstream = mock(FilterChain.class);
            filter.doFilter(request(path), new MockHttpServletResponse(), downstream);
            verify(downstream).doFilter(any(), any());
        }
        verify(permission, times(2)).canAccess(101L, user.roles(), "/faculty/employment-rate-improvement-achievements");
    }

    @Test
    void authenticationCatchDoesNotConvertBusinessConflictToUnauthorized() throws Exception {
        when(auth.currentUser("TEST-SESSION")).thenReturn(user);
        when(permission.canAccess(any(), any(), any())).thenReturn(true);
        FilterChain downstream = mock(FilterChain.class);
        ConflictException conflict = new ConflictException("PERIOD_NOT_ACTIVE: 입력기간 밖");
        doThrow(conflict).when(downstream).doFilter(any(), any());
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(request("/api/business/employment-rate-improvements"),
                response, downstream)).isSameAs(conflict);
        assertThat(response.getStatus()).isNotEqualTo(401);
    }

    @Test
    void newFilterDenialsHaveObjectFieldsAndSameGeneratedRequestId() throws Exception {
        when(auth.currentUser("TEST-SESSION")).thenReturn(user);
        FilterChain downstream = mock(FilterChain.class);
        var response = new MockHttpServletResponse();
        new EducationAchievementRequestFilter().doFilter(request("/api/business/employment-rate-improvements"),
                response, (req, res) -> filter.doFilter(req, res, downstream));
        var body = json.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.path("error").path("fields").isObject()).isTrue();
        assertThat(body.path("meta").path("requestId").asText()).isNotBlank()
                .isEqualTo(response.getHeader("X-Request-Id"));
        verifyNoInteractions(downstream);
    }

    @Test
    void invalidSessionStillReturnsUnauthorizedWhileLegacyFieldsRemainAnArray() throws Exception {
        when(auth.currentUser("TEST-SESSION")).thenThrow(new IllegalStateException("invalid session"));
        FilterChain downstream = mock(FilterChain.class);
        var response = new MockHttpServletResponse();
        filter.doFilter(request("/api/business/lecture-achievements"), response, downstream);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(json.readTree(response.getContentAsString()).path("error").path("fields").isArray()).isTrue();
        verifyNoInteractions(downstream);
    }

    private MockHttpServletRequest request(String path) {
        var request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"));
        return request;
    }
}

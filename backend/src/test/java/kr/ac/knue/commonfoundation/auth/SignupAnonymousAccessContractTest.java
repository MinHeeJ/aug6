package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Exercises the real anonymous-access boundary, not a substitute signup controller. */
class SignupAnonymousAccessContractTest {
    private final AuthService authService = mock(AuthService.class);
    private final EffectivePermissionService permissions = mock(EffectivePermissionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AuthenticationFilter filter = new AuthenticationFilter(authService, permissions, objectMapper);

    @ParameterizedTest
    @CsvSource({
            "POST, /api/v1/auth/signup",
            "GET, /api/v1/auth/check-userid"
    })
    void documentedAnonymousOperationReachesHandlerChainWithoutSession(String method, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // Handler status/body belongs to the implementation phase; this only proves filter reachability.
        verify(chain).doFilter(request, response);
        verifyNoInteractions(authService, permissions);
        assertThat(response.getHeader("Set-Cookie")).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /api/v1/auth/signup",
            "POST, /api/v1/auth/check-userid",
            "POST, /api/v1/auth/signup/extra",
            "GET, /api/v1/auth/check-userid/extra",
            "GET, /api/v1/auth/other",
            "GET, /api/auth/me",
            "POST, /api/auth/logout",
            "GET, /api/admin/users",
            "GET, /api/business/lecture-achievements"
    })
    void anonymousAllowlistDoesNotExposeOtherMethodsOrProtectedPaths(String method, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("error").path("code").asText()).isEqualTo("UNAUTHENTICATED");
        verify(chain, never()).doFilter(request, response);
        verifyNoInteractions(authService, permissions);
        assertThat(response.getHeader("Set-Cookie")).isNull();
    }
}

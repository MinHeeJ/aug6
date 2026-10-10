package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.stream.Stream;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Checks that business collections and suffixes resolve to the same session-menu permission boundary. */
class EducationAchievementMenuAccessTest {
    @ParameterizedTest
    @MethodSource("routes")
    void authenticatedRequestUsesCanonicalMenuBeforeDispatch(String apiPath, String uiPath, String role)
            throws Exception {
        AuthService auth = mock(AuthService.class);
        EffectivePermissionService permissions = mock(EffectivePermissionService.class);
        CurrentUser user = user(role);
        when(auth.currentUser("education-session")).thenReturn(user);
        when(permissions.canAccess(user.userId(), user.roles(), uiPath)).thenReturn(true);
        AuthenticationFilter filter = new AuthenticationFilter(auth, permissions, new ObjectMapper());
        MockHttpServletRequest request = request(apiPath, true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute("currentUser")).isSameAs(user);
        verify(permissions).canAccess(user.userId(), user.roles(), uiPath);
        verify(chain).doFilter(request, response);
    }

    @ParameterizedTest
    @MethodSource("routes")
    void deniedMenuCannotReachTheController(String apiPath, String uiPath, String role) throws Exception {
        AuthService auth = mock(AuthService.class);
        EffectivePermissionService permissions = mock(EffectivePermissionService.class);
        CurrentUser user = user(role);
        when(auth.currentUser("education-session")).thenReturn(user);
        when(permissions.canAccess(user.userId(), user.roles(), uiPath)).thenReturn(false);
        ObjectMapper json = new ObjectMapper();
        AuthenticationFilter filter = new AuthenticationFilter(auth, permissions, json);
        MockHttpServletRequest request = request(apiPath, true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(json.readTree(response.getContentAsString()).path("error").path("code").asText())
                .isEqualTo("FORBIDDEN");
        verify(permissions).canAccess(user.userId(), user.roles(), uiPath);
        verify(chain, never()).doFilter(any(), any());
    }

    @ParameterizedTest
    @MethodSource("routes")
    void anonymousRequestFailsBeforeMenuOrController(String apiPath, String uiPath, String role) throws Exception {
        AuthService auth = mock(AuthService.class);
        EffectivePermissionService permissions = mock(EffectivePermissionService.class);
        ObjectMapper json = new ObjectMapper();
        AuthenticationFilter filter = new AuthenticationFilter(auth, permissions, json);
        MockHttpServletRequest request = request(apiPath, false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(json.readTree(response.getContentAsString()).path("error").path("code").asText())
                .isEqualTo("UNAUTHENTICATED");
        verify(permissions, never()).canAccess(any(), any(), any());
        verify(chain, never()).doFilter(any(), any());
    }

    private static Stream<Arguments> routes() {
        Stream<Arguments> individual = Stream.of(
                "employment-rate-improvements", "course-operations", "lecture-improvements",
                "employment-rate-achievements", "lecture-improvements/82", "course-operations/82",
                "employment-rate-improvements/82", "employment-rate-achievements/82")
                .flatMap(resource -> Stream.of("R01", "R02", "R04", "R09").map(role -> Arguments.of(
                        "/api/business/" + resource,
                        "/faculty/education/" + resource.split("/")[0],
                        role)));
        Stream<Arguments> excelAndBulk = Stream.of(
                "download", "excel-uploads", "excel-uploads/template", "excel-uploads/histories",
                "excel-uploads/upload-82/errors", "excel-uploads/upload-82/errors/download",
                "excel-uploads/upload-82/commit", "bulk-jobs", "bulk-jobs/job-82")
                .flatMap(suffix -> Stream.of("R07", "R09").map(role -> Arguments.of(
                        "/api/business/employment-rate-achievements/" + suffix,
                        "/faculty/education/employment-rate-achievements",
                        role)));
        return Stream.concat(individual, excelAndBulk);
    }

    private CurrentUser user(String role) {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of(role), List.of());
    }

    private MockHttpServletRequest request(String path, boolean authenticated) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        if (authenticated) {
            request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "education-session"));
        }
        return request;
    }
}

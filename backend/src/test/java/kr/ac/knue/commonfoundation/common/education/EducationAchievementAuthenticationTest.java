package kr.ac.knue.commonfoundation.common.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Checks only the existing session boundary; no dummy controller or addFilters=false bypass
 * is used. Authenticated business permissions remain the final wiring slice's responsibility.
 */
class EducationAchievementAuthenticationTest {
    @Test
    void authenticatedCollectionsAndDescendantsUseTheSeededMenuRoute() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissions = mock(EffectivePermissionService.class);
        CurrentUser user = new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01"), List.of());
        when(authService.currentUser("valid-session")).thenReturn(user);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissions, new ObjectMapper());

        for (EducationAchievementContract.Surface surface : EducationAchievementContract.SURFACES) {
            when(permissions.canAccess(user.userId(), user.roles(), surface.uiRoute())).thenReturn(true);
            for (String suffix : new String[] {"", "/23", "/download", "/excel-uploads/histories"}) {
                MockHttpServletRequest request = new MockHttpServletRequest("GET", surface.apiPath() + suffix);
                request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "valid-session"));
                MockHttpServletResponse response = new MockHttpServletResponse();
                FilterChain chain = mock(FilterChain.class);

                filter.doFilter(request, response, chain);

                verify(chain).doFilter(request, response);
                assertThat(request.getAttribute("currentUser")).isSameAs(user);
            }
        }
    }

    @Test
    void anonymousCollectionAndDescendantRequestsStopBeforeAnyDomainHandler() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissions = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissions, new ObjectMapper());
        MockMvc mvc = MockMvcBuilders.standaloneSetup().addFilters(filter).build();

        for (EducationAchievementContract.Surface surface : EducationAchievementContract.SURFACES) {
            for (String suffix : new String[] {"", "/23", "/download"}) {
                mvc.perform(get(surface.apiPath() + suffix)
                                .header("X-Request-Id", EducationAchievementTestFixtures.REQUEST_ID))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                        .andExpect(jsonPath("$.data").doesNotExist());
            }
        }
        verifyNoInteractions(authService, permissions);
    }

    @Test
    void invalidSessionDoesNotExposeRepositoryOrCredentialDetails() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissions = mock(EffectivePermissionService.class);
        when(authService.currentUser("invalid-session"))
                .thenThrow(new IllegalStateException("private database host; internal credential diagnostic"));
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissions, new ObjectMapper());
        MockMvc mvc = MockMvcBuilders.standaloneSetup().addFilters(filter).build();

        String body = mvc.perform(get("/api/business/employment-rate-achievements/excel-uploads/histories")
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "invalid-session")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("database", "credential", "IllegalStateException");
        verifyNoInteractions(permissions);
    }
}

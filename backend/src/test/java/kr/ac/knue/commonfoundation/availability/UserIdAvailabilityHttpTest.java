package kr.ac.knue.commonfoundation.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import kr.ac.knue.commonfoundation.signup.SignupController;
import kr.ac.knue.commonfoundation.signup.SignupExceptionHandler;
import kr.ac.knue.commonfoundation.signup.SignupMapper;
import kr.ac.knue.commonfoundation.signup.SignupPasswordHasher;
import kr.ac.knue.commonfoundation.signup.SignupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Covers the existing availability handler through its real anonymous filter and validation service. */
class UserIdAvailabilityHttpTest {
    private SignupMapper mapper;
    private SignupPasswordHasher hasher;
    private AuthService auth;
    private EffectivePermissionService permissions;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mapper = mock(SignupMapper.class);
        hasher = mock(SignupPasswordHasher.class);
        auth = mock(AuthService.class);
        permissions = mock(EffectivePermissionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SignupController(new SignupService(mapper, hasher)))
                .setControllerAdvice(new SignupExceptionHandler(), new GlobalExceptionHandler())
                .addFilters(new AuthenticationFilter(auth, permissions, new ObjectMapper()))
                .build();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void exactAnonymousGetReachesService(boolean reserved) throws Exception {
        when(mapper.existsLoginId("validuser1")).thenReturn(reserved);
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", "validuser1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(!reserved))
                .andExpect(header().doesNotExist("Set-Cookie"));
        verifyNoInteractions(auth, permissions, hasher);
    }

    @Test
    void absentQueryStillReturnsFieldValidationThroughAnonymousFilter() throws Exception {
        mvc.perform(get("/api/v1/auth/check-userid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("userId"))
                .andExpect(header().doesNotExist("Set-Cookie"));
        verifyNoInteractions(mapper, hasher, auth, permissions);
    }

    @Test
    void anonymousExemptionDoesNotCoverOtherMethodsOrPaths() throws Exception {
        mvc.perform(post("/api/v1/auth/check-userid").param("userId", "validuser1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/v1/auth/check-userid/extra").param("userId", "validuser1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(mapper, hasher, auth, permissions);
    }

    @Test
    void databaseFailureCannotExposeSqlOrAccountDetails() throws Exception {
        when(mapper.existsLoginId("validuser1"))
                .thenThrow(new DataAccessResourceFailureException("SELECT password_hash FROM users private-bind"));
        String body = mvc.perform(get("/api/v1/auth/check-userid").param("userId", "validuser1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.data.available").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("SELECT", "password_hash", "private-bind", "validuser1");
        verifyNoInteractions(auth, permissions, hasher);
    }
}

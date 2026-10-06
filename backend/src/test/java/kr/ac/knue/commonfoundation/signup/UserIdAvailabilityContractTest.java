package kr.ac.knue.commonfoundation.signup;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Exercises the actual controller and service; PostgreSQL coverage lives in SignupIntegrationTest. */
class UserIdAvailabilityContractTest {
    private SignupMapper mapper;
    private SignupPasswordHasher hasher;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mapper = mock(SignupMapper.class);
        hasher = mock(SignupPasswordHasher.class);
        mvc = MockMvcBuilders.standaloneSetup(new SignupController(new SignupService(mapper, hasher)))
                .setControllerAdvice(new SignupExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsAvailabilityWithoutWritesOrSession(boolean exists) throws Exception {
        when(mapper.existsLoginId("validuser1")).thenReturn(exists);
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", "validuser1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(!exists))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.meta.traceId").isString())
                .andExpect(header().doesNotExist("Set-Cookie"));
        verify(mapper).existsLoginId("validuser1");
        verifyNoMoreInteractions(mapper);
        verifyNoInteractions(hasher);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "abc", "1abc", "Abcd", "ab-c", "abcdefghijklmnopqrstu", " validuser1 "})
    void invalidIdReturns400FieldErrorBeforeDatabaseLookup(String userId) throws Exception {
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("userId"))
                .andExpect(header().doesNotExist("Set-Cookie"));
        verifyNoInteractions(mapper, hasher);
    }

    @Test
    void missingIdReturns400WithUserIdField() throws Exception {
        mvc.perform(get("/api/v1/auth/check-userid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("userId"));
        verifyNoInteractions(mapper, hasher);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "abcdefghijklmnopqrst"})
    void acceptsBothLengthBoundaries(String userId) throws Exception {
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
        verify(mapper).existsLoginId(userId);
        verifyNoMoreInteractions(mapper);
    }
}

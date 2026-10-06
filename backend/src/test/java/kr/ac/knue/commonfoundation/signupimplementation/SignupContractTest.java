package kr.ac.knue.commonfoundation.signupimplementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// HTTP/controller + real ordered service tests. SQL and filter registration are not claimed by this slice.
class SignupContractTest {
    private SignupMapper mapper;
    private SignupPasswordEncoder encoder;
    private MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();
    private String password;

    @BeforeEach
    void setup() {
        mapper = mock(SignupMapper.class);
        encoder = new SignupPasswordEncoder();
        mvc = MockMvcBuilders.standaloneSetup(new SignupController(new SignupService(mapper, encoder))).build();
        password = "Aa1!" + UUID.randomUUID();
        when(mapper.insertAccount(anyString(), anyString(), anyString())).thenReturn(912L);
        when(mapper.insertDefaultRole(912L)).thenReturn(1);
    }

    @Test
    void approvedFixtureIsLoadedFromClasspath() throws Exception {
        try (var stream = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThat(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .contains("operationId: signup", "operationId: checkUserIdAvailability");
        }
    }

    @Test
    void createReturns201NoCookieAndOnlyReceiptWithHashAndGeneratedRoleKey() throws Exception {
        String response = submit(valid()).andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("newteacher"))
                .andExpect(jsonPath("$.data.message").value("가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain(password, "passwordConfirm", "passwordHash");
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(mapper).insertAccount(eq("newteacher"), hash.capture(), eq("teacher@example.invalid"));
        assertThat(hash.getValue()).startsWith("$argon2id$");
        assertThat(encoder.matches(password, hash.getValue())).isTrue();
        var order = inOrder(mapper);
        order.verify(mapper).insertAccount(eq("newteacher"), anyString(), eq("teacher@example.invalid"));
        order.verify(mapper).insertDefaultRole(912L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"userId", "password", "passwordConfirm", "email"})
    void missingFieldsFailBeforePersistence(String field) throws Exception {
        Map<String, String> input = valid();
        input.remove(field);
        submit(input).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value(field));
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1abcd", "Uppercase", "abc_123", "abcdefghijklmnopqrstu"})
    void invalidUserIdNeverQueriesOrWrites(String value) throws Exception {
        Map<String, String> input = valid();
        input.put("userId", value);
        submit(input).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("userId"));
        verifyNoInteractions(mapper);
    }

    @Test
    void duplicateUserIdPrecedesInvalidEmailAndPassword() throws Exception {
        when(mapper.loginIdExists("newteacher")).thenReturn(true);
        Map<String, String> input = valid();
        input.put("email", "not-an-email");
        input.put("passwordConfirm", "different");
        submit(input).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.fields[0].field").value("userId"))
                .andExpect(jsonPath("$.error.message").value("이미 사용 중인 아이디입니다."));
        verify(mapper, never()).emailExists(anyString());
        verify(mapper, never()).insertAccount(anyString(), anyString(), anyString());
    }

    @Test
    void duplicateNormalizedEmailPrecedesMismatchAndDoesNotWrite() throws Exception {
        when(mapper.emailExists("teacher@example.invalid")).thenReturn(true);
        Map<String, String> input = valid();
        input.put("passwordConfirm", "different");
        submit(input).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.fields[0].field").value("email"))
                .andExpect(jsonPath("$.error.message").value("이미 등록된 이메일입니다."));
        verify(mapper, never()).insertAccount(anyString(), anyString(), anyString());
    }

    @Test
    void mismatchRejectsWithFieldAndNoInsert() throws Exception {
        Map<String, String> input = valid();
        input.put("passwordConfirm", "different");
        submit(input).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("passwordConfirm"));
        verify(mapper, never()).insertAccount(anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1!", "onlylowercase", "lower12345", "lowercase spaces"})
    void passwordRulesAreEnforcedAtServer(String value) throws Exception {
        Map<String, String> input = valid();
        input.put("password", value);
        input.put("passwordConfirm", value);
        submit(input).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("password"));
        verify(mapper, never()).insertAccount(anyString(), anyString(), anyString());
    }

    @Test
    void emailFormatAndLengthAreValidated() throws Exception {
        for (String email : new String[]{"bad", "a".repeat(250) + "@example.invalid"}) {
            Map<String, String> input = valid();
            input.put("email", email);
            submit(input).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[0].field").value("email"));
        }
        verify(mapper, never()).insertAccount(anyString(), anyString(), anyString());
    }

    @Test
    void availabilityReturnsTrueFalseAndValidatesMissingQuery() throws Exception {
        when(mapper.loginIdExists("takenuser")).thenReturn(true);
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", "freeuser"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", "takenuser"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
        mvc.perform(get("/api/v1/auth/check-userid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("userId"));
        verify(mapper, never()).insertAccount(anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"uq_users_signup_email", "users_login_id_key"})
    void uniqueRaceBecomesSafeFieldConflict(String constraint) throws Exception {
        when(mapper.insertAccount(anyString(), anyString(), anyString()))
                .thenThrow(new DataIntegrityViolationException(constraint + " secret SQL " + password));
        String field = constraint.equals("users_login_id_key") ? "userId" : "email";
        String response = submit(valid()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.fields[0].field").value(field))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain(password, "SQL", "uq_users");
        verify(mapper, never()).insertDefaultRole(any());
    }

    @Test
    void roleFailureDoesNotLeakDatabaseDiagnostics() throws Exception {
        when(mapper.insertDefaultRole(any())).thenThrow(new DataIntegrityViolationException("internal " + password));
        String response = submit(valid()).andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain(password, "internal ");
    }

    @Test
    void malformedBodyAndRequestSerializationDoNotExposeCredentials() throws Exception {
        mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content("{invalid"))
                .andExpect(status().isBadRequest());
        SignupRequest request = new SignupRequest("newteacher", password, password, "teacher@example.invalid");
        assertThat(request.toString()).doesNotContain(password);
        assertThat(json.writeValueAsString(request)).doesNotContain(password, "passwordConfirm", "password");
        String first = encoder.encode(password);
        assertThat(encoder.encode(password)).isNotEqualTo(first);
        assertThat(encoder.matches(password + "x", first)).isFalse();
        assertThat(encoder.matches(password, "$argon2id$broken")).isFalse();
    }

    private Map<String, String> valid() {
        Map<String, String> input = new HashMap<>();
        input.put("userId", "newteacher");
        input.put("password", password);
        input.put("passwordConfirm", password);
        input.put("email", "Teacher@Example.Invalid");
        return input;
    }

    private ResultActions submit(Map<String, String> input) throws Exception {
        return mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input)));
    }
}

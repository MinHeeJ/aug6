package kr.ac.knue.commonfoundation.signup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP and real validation/hashing coverage; only persistence is mocked in this focused slice. */
class SignupContractTest {
    private final ObjectMapper json = new ObjectMapper();
    private SignupMapper mapper;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mapper = mock(SignupMapper.class);
        SignupService service = new SignupService(mapper, new SignupPasswordHasher());
        mvc = MockMvcBuilders.standaloneSetup(new SignupController(service))
                .setControllerAdvice(new SignupExceptionHandler(), new GlobalExceptionHandler())
                .build();
        when(mapper.insertUser(anyString(), anyString(), anyString())).thenReturn(901L);
        when(mapper.insertDefaultRole(901L)).thenReturn(1);
    }

    @Test
    void anonymousCreateUsesGeneratedIdentityAndArgon2WithoutSessionOrSecretResponse() throws Exception {
        Map<String, String> request = request();
        MvcResult result = send(request)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("signupuser"))
                .andExpect(jsonPath("$.data.message").value("가입이 완료되었습니다."))
                .andExpect(jsonPath("$.meta.traceId").isString())
                .andReturn();
        var hash = org.mockito.ArgumentCaptor.forClass(String.class);
        var order = inOrder(mapper);
        order.verify(mapper).insertUser(eq("signupuser"), eq("signup@example.test"), hash.capture());
        order.verify(mapper).insertDefaultRole(901L);
        assertThat(hash.getValue()).startsWith("$argon2id$");
        assertThat(new Argon2PasswordEncoder(16, 32, 1, 19456, 2)
                .matches(request.get("password"), hash.getValue())).isTrue();
        assertThat(json.readTree(result.getResponse().getContentAsString()).path("data").size()).isEqualTo(2);
        assertSafe(result, request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"userId", "password", "passwordConfirm", "email"})
    void missingFieldsRejectBeforeReadingDatabase(String field) throws Exception {
        Map<String, String> request = request();
        request.remove(field);
        error(request, 400, field);
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1abc", "Abcd", "ab-c", "abcdefghijklmnopqrstu"})
    void idRulesRejectWithoutPersistence(String id) throws Exception {
        Map<String, String> request = request();
        request.put("userId", id);
        error(request, 400, "userId");
        verifyNoInteractions(mapper);
    }

    @Test
    void duplicateIdPrecedesEmailAndPasswordValidation() throws Exception {
        when(mapper.existsLoginId("signupuser")).thenReturn(true);
        Map<String, String> request = request();
        request.put("email", "invalid");
        request.put("passwordConfirm", "different");
        error(request, 409, "userId");
        verify(mapper, never()).existsEmail(anyString());
        verify(mapper, never()).insertUser(anyString(), anyString(), anyString());
    }

    @Test
    void invalidEmailPrecedesMismatch() throws Exception {
        Map<String, String> request = request();
        request.put("email", "invalid");
        request.put("passwordConfirm", "different");
        error(request, 400, "email");
    }

    @Test
    void normalizedEmailConflictPrecedesMismatch() throws Exception {
        when(mapper.existsEmail("signup@example.test")).thenReturn(true);
        Map<String, String> request = request();
        request.put("passwordConfirm", "different");
        error(request, 409, "email");
        verify(mapper, never()).insertUser(anyString(), anyString(), anyString());
    }

    @Test
    void mismatchHasFieldErrorAndNoWrites() throws Exception {
        Map<String, String> request = request();
        request.put("passwordConfirm", "different");
        error(request, 400, "passwordConfirm");
        verify(mapper, never()).insertUser(anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Aa1!", "abcdefgh", "abcdefgh1", "ABCDEFGH1", "signupuser"})
    void weakPasswordsReject(String password) throws Exception {
        Map<String, String> request = request();
        request.put("password", password);
        request.put("passwordConfirm", password);
        error(request, 400, "password");
        verify(mapper, never()).insertUser(anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd123!", "Aa123456", "Abcdefg!", "ABCD123!"})
    void anyThreeOfFourPasswordCategoriesAtMinimumLengthAreAccepted(String password) throws Exception {
        Map<String, String> request = request();
        request.put("password", password);
        request.put("passwordConfirm", password);
        MvcResult result = send(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("signupuser"))
                .andExpect(jsonPath("$.data.message").value("가입이 완료되었습니다."))
                .andReturn();
        verify(mapper).insertUser(eq("signupuser"), eq("signup@example.test"), anyString());
        verify(mapper).insertDefaultRole(901L);
        assertSafe(result, request);
    }

    @Test
    void uniqueRaceReportsSafeFieldConflict() throws Exception {
        when(mapper.insertUser(anyString(), anyString(), anyString())).thenReturn(null);
        when(mapper.existsLoginId("signupuser")).thenReturn(false, true);
        error(request(), 409, "userId");
        verify(mapper, never()).insertDefaultRole(any());
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void databaseFailureCannotLeakBindingsOrHash(CapturedOutput output) throws Exception {
        Map<String, String> request = request();
        when(mapper.insertDefaultRole(901L)).thenThrow(new DataIntegrityViolationException(
                "secret SQL " + request.get("password") + " $argon2id$"
        ));
        MvcResult result = send(request).andExpect(status().isInternalServerError()).andReturn();
        assertSafe(result, request);
        assertThat(output.getAll()).doesNotContain(request.get("password"), "secret SQL", "$argon2id$");
    }

    @Test
    void malformedInputDoesNotEchoSecretAndRequestDiagnosticsAreRedacted() throws Exception {
        Map<String, String> request = request();
        String secret = request.get("password");
        mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + secret + "\",\"email\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(secret))));
        assertThat(new SignupRequest("signupuser", secret, secret, "signup@example.test").toString())
                .isEqualTo("SignupRequest[redacted]");
    }

    private org.springframework.test.web.servlet.ResultActions send(Map<String, String> request) throws Exception {
        return mvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(request)));
    }

    private void error(Map<String, String> request, int status, String field) throws Exception {
        MvcResult result = send(request)
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(status == 409 ? "CONFLICT" : "VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value(field))
                .andReturn();
        assertSafe(result, request);
    }

    private static void assertSafe(MvcResult result, Map<String, String> request) throws Exception {
        String body = result.getResponse().getContentAsString();
        for (String field : new String[]{"password", "passwordConfirm"}) {
            if (request.containsKey(field)) {
                assertThat(body).doesNotContain(request.get(field));
            }
        }
        assertThat(body).doesNotContain("password_hash", "$argon2id$", "secret SQL");
    }

    private static Map<String, String> request() {
        Map<String, String> request = new LinkedHashMap<>();
        String password = UUID.randomUUID() + "Aa9!";
        request.put("userId", "signupuser");
        request.put("password", password);
        request.put("passwordConfirm", password);
        request.put("email", "SIGNUP@EXAMPLE.TEST");
        return request;
    }
}

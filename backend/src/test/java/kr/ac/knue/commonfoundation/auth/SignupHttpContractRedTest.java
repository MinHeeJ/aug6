package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.CommonFoundationApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** Real application HTTP contract tests; the runner must supply a disposable PostgreSQL database. */
@SpringBootTest(classes = CommonFoundationApplication.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
@Transactional
class SignupHttpContractRedTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void anonymousSignupReturns201WithoutSessionOrSecrets() throws Exception {
        Map<String, String> request = validRequest();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(request.get("userId")))
                .andExpect(jsonPath("$.data.message").value("가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordConfirm").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.meta.traceId").isString())
                .andReturn();
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString()).path("data").size())
                .as("only the public userId and message belong to the success DTO")
                .isEqualTo(2);
        assertNoSecrets(result, request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"userId", "password", "passwordConfirm", "email"})
    void missingRequiredFieldReturns400WithFieldName(String field) throws Exception {
        Map<String, String> request = validRequest();
        request.remove(field);
        assertError(request, 400, field, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "abcdefghijklmnopqrstu", "1abc", "Abcd", "ab-c"})
    void signupRejectsInvalidUserId(String userId) throws Exception {
        Map<String, String> request = validRequest();
        request.put("userId", userId);
        assertError(request, 400, "userId", null);
    }

    @Test
    void duplicateIdTakesPrecedenceOverInvalidEmailAndPasswordMismatch() throws Exception {
        Map<String, String> request = validRequest();
        request.put("userId", "testuser1");
        request.put("email", "invalid-email");
        request.put("passwordConfirm", "different");
        assertError(request, 409, "userId", "이미 사용 중인 아이디입니다.");
    }

    @Test
    void invalidEmailReturns400BeforePasswordMismatch() throws Exception {
        Map<String, String> request = validRequest();
        request.put("email", "invalid-email");
        request.put("passwordConfirm", "different");
        assertError(request, 400, "email", "올바른 이메일 형식이 아닙니다.");
    }

    @Test
    void duplicateEmailIsCaseInsensitiveAndTakesPrecedenceOverPasswordMismatch() throws Exception {
        String fixtureEmail = jdbc.queryForObject("""
                SELECT u.email
                FROM users u
                WHERE u.login_id = ?
                """, String.class, "testuser2");
        assertThat(fixtureEmail).as("T002 testuser2 email fixture prerequisite").isNotBlank();
        Map<String, String> request = validRequest();
        request.put("email", fixtureEmail.toUpperCase(java.util.Locale.ROOT));
        request.put("passwordConfirm", "different");
        assertError(request, 409, "email", "이미 등록된 이메일입니다.");
    }

    @Test
    void passwordMismatchReturns400WithoutEchoingTheSubmittedPassword() throws Exception {
        Map<String, String> request = validRequest();
        request.put("passwordConfirm", "different");
        assertError(request, 400, "passwordConfirm", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Aa1!", "abcdefgh", "abcdefgh1", "ABCDEFGH1"})
    void passwordLengthAndCategoryRulesAreEnforcedByServer(String password) throws Exception {
        Map<String, String> request = validRequest();
        request.put("password", password);
        request.put("passwordConfirm", password);
        assertError(request, 400, "password", null);
    }

    @Test
    void availabilityReturnsTrueForUnusedIdAndFalseForFixtureWithoutSession() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-userid").param("userId", newUserId()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));
        mockMvc.perform(get("/api/v1/auth/check-userid").param("userId", "testuser1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "abcdefghijklmnopqrstu", "1abc", "Abcd", "ab-c"})
    void availabilityRejectsInvalidUserIdWithFieldError(String userId) throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-userid").param("userId", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'userId')]").isNotEmpty());
    }

    @Test
    void availabilityRequiresQueryParameter() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-userid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'userId')]").isNotEmpty());
    }

    @Test
    void existingSessionAndAdminPathsRemainProtected() throws Exception {
        for (String path : new String[]{"/api/auth/me", "/api/admin/users"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        }
    }

    private void assertError(Map<String, String> request, int expectedStatus, String field, String message)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().is(expectedStatus))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(expectedStatus == 409 ? "CONFLICT" : "VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == '" + field + "')]").isNotEmpty())
                .andReturn();
        if (message != null) {
            assertThat(objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("error").path("message").asText()).isEqualTo(message);
        }
        assertNoSecrets(result, request);
    }

    private static void assertNoSecrets(MvcResult result, Map<String, String> request) throws Exception {
        String response = result.getResponse().getContentAsString();
        for (String field : new String[]{"password", "passwordConfirm"}) {
            String submitted = request.get(field);
            if (submitted != null && !submitted.isBlank()) {
                assertThat(response).doesNotContain(submitted);
            }
        }
        assertThat(response).doesNotContain("passwordHash", "password_hash", "$argon2id$", "org.postgresql");
    }

    private static Map<String, String> validRequest() {
        String userId = newUserId();
        // Disposable test input, never a real credential; randomized to avoid seed/password coupling.
        String password = UUID.randomUUID() + "Aa9!";
        Map<String, String> request = new LinkedHashMap<>();
        request.put("userId", userId);
        request.put("password", password);
        request.put("passwordConfirm", password);
        request.put("email", userId + "@example.test");
        return request;
    }

    private static String newUserId() {
        return "contract" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.CommonFoundationApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** Real anonymous HTTP acceptance checks. Test transactions isolate data; they do not prove committed durability. */
@SpringBootTest(classes = CommonFoundationApplication.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
@Transactional
class SignupSmokeAcceptanceTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void validSignupStoresHashAndOnlyR01WithoutAutomaticLogin() throws Exception {
        Map<String, String> request = validRequest();
        availability(request.get("userId"), true);
        MvcResult result = submit(request, 201);
        var data = json.readTree(result.getResponse().getContentAsByteArray()).path("data");
        assertThat(data.size()).isEqualTo(2);
        assertThat(data.path("userId").asText()).isEqualTo(request.get("userId"));
        assertThat(data.path("message").asText()).isEqualTo("가입이 완료되었습니다.");

        Map<String, Object> account = jdbc.queryForMap("""
                SELECT u.user_id, u.password_hash, u.email
                FROM users u
                WHERE u.login_id = ?
                """, request.get("userId"));
        assertThat(account.get("email")).isEqualTo(request.get("email").toLowerCase(Locale.ROOT));
        String hash = (String) account.get("password_hash");
        assertThat(hash != null && hash.startsWith("$argon2id$")).isTrue();
        assertThat(new Argon2PasswordEncoder(16, 32, 1, 19456, 2)
                .matches(request.get("password"), hash)).isTrue();
        assertThat(jdbc.queryForList("""
                SELECT ur.role_code
                FROM user_roles ur
                WHERE ur.user_id = ?
                ORDER BY ur.assignment_id
                """, String.class, account.get("user_id"))).containsExactly("R01");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sessions WHERE user_id = ?",
                Long.class, account.get("user_id"))).isZero();
        availability(request.get("userId"), false);
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @ParameterizedTest
    @CsvSource({
            "userId,testuser1,409,userId",
            "email,testuser2@EXAMPLE.COM,409,email",
            "passwordConfirm,different,400,passwordConfirm",
            "email,invalid-email,400,email",
            "userId,1abc,400,userId"
    })
    void invalidOrDuplicateSignupHasFieldErrorAndNoWrites(
            String changedField, String value, int expectedStatus, String errorField
    ) throws Exception {
        Map<String, String> request = validRequest();
        request.put(changedField, value);
        long usersBefore = count("users");
        long rolesBefore = count("user_roles");
        long sessionsBefore = count("sessions");
        MvcResult result = submit(request, expectedStatus);
        var error = json.readTree(result.getResponse().getContentAsByteArray()).path("error");
        assertThat(error.path("code").asText())
                .isEqualTo(expectedStatus == 409 ? "CONFLICT" : "VALIDATION_ERROR");
        assertThat(error.path("fields").get(0).path("field").asText()).isEqualTo(errorField);
        assertThat(count("users")).isEqualTo(usersBefore);
        assertThat(count("user_roles")).isEqualTo(rolesBefore);
        assertThat(count("sessions")).isEqualTo(sessionsBefore);
    }

    @Test
    void missingFieldsAndWeakPasswordAreRejectedBeforePersistence() throws Exception {
        long before = count("users");
        MvcResult missing = submit(Map.of(), 400);
        var fields = json.readTree(missing.getResponse().getContentAsByteArray()).path("error").path("fields");
        assertThat(fields.size()).isEqualTo(4);
        for (int index = 0; index < 4; index++) {
            assertThat(fields.get(index).path("field").asText())
                    .isEqualTo(List.of("userId", "password", "passwordConfirm", "email").get(index));
        }
        Map<String, String> weak = validRequest();
        weak.put("password", "abc");
        weak.put("passwordConfirm", "abc");
        MvcResult invalid = submit(weak, 400);
        assertThat(json.readTree(invalid.getResponse().getContentAsByteArray())
                .path("error").path("fields").get(0).path("field").asText()).isEqualTo("password");
        assertThat(count("users")).isEqualTo(before);
    }

    @Test
    void signupDoesNotMakeProtectedOrExcludedAuthPathsAnonymous() throws Exception {
        for (String path : List.of("/api/auth/me", "/api/admin/users", "/api/v1/auth/reset-password",
                "/api/v1/auth/verify-email")) {
            mvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        }
        mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/signup"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/check-userid"))
                .andExpect(status().isUnauthorized());
    }

    private void availability(String userId, boolean available) throws Exception {
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(available))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    private MvcResult submit(Map<String, String> request, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.success").value(expectedStatus == 201))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();
        String response = result.getResponse().getContentAsString();
        for (String secret : List.of("password", "passwordConfirm")) {
            String value = request.get(secret);
            if (value != null) {
                assertThat(response.contains(value)).as("response must not echo credentials").isFalse();
            }
        }
        assertThat(response.contains("$argon2id$")).isFalse();
        assertThat(response.contains("password_hash")).isFalse();
        return result;
    }

    private long count(String table) {
        // Only file-local constant table names reach this helper; no request input is interpolated.
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private Map<String, String> validRequest() {
        String id = "accept" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String password = UUID.randomUUID() + "Aa9!";
        return new LinkedHashMap<>(Map.of(
                "userId", id,
                "password", password,
                "passwordConfirm", password,
                "email", id.toUpperCase(Locale.ROOT) + "@EXAMPLE.TEST"
        ));
    }
}

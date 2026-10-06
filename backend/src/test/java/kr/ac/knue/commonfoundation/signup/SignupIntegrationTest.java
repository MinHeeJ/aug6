package kr.ac.knue.commonfoundation.signup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.CommonFoundationApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Runner-owned PostgreSQL HTTP/persistence checks; requests commit or roll back in real service transactions. */
@SpringBootTest(classes = CommonFoundationApplication.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
class SignupIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    private final List<String> ids = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String id : ids) {
            jdbc.update("""
                    DELETE FROM user_roles ur
                    USING users u
                    WHERE ur.user_id = u.user_id
                        AND u.login_id = ?
                    """, id);
            jdbc.update("DELETE FROM users WHERE login_id = ?", id);
        }
    }

    @Test
    void anonymousCreateCommitsLowercaseEmailArgon2AndOnlyR01() throws Exception {
        Map<String, String> request = request();
        create(request, 201);
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT u.user_id, u.password_hash, u.email, u.status, u.system_use_yn
                FROM users u
                WHERE u.login_id = ?
                """, request.get("userId"));
        assertThat(row.get("email")).isEqualTo(request.get("email").toLowerCase(Locale.ROOT));
        assertThat(row.get("status")).isEqualTo("ACTIVE");
        assertThat(row.get("system_use_yn")).isEqualTo("Y");
        String hash = (String) row.get("password_hash");
        assertThat(hash).startsWith("$argon2id$");
        assertThat(new Argon2PasswordEncoder(16, 32, 1, 19456, 2)
                .matches(request.get("password"), hash)).isTrue();
        List<Map<String, Object>> roles = jdbc.queryForList("""
                SELECT ur.role_code, ur.assignment_type, ur.status, ur.approver_user_id
                FROM user_roles ur
                WHERE ur.user_id = ?
                ORDER BY ur.assignment_id
                """, row.get("user_id"));
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0)).containsEntry("role_code", "R01")
                .containsEntry("assignment_type", "MANUAL")
                .containsEntry("status", "ACTIVE")
                .containsEntry("approver_user_id", null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sessions WHERE user_id = ?",
                Long.class, row.get("user_id"))).isZero();
    }

    @Test
    void duplicateEmailLeavesOriginalAccountAndRolesUnchanged() throws Exception {
        Map<String, String> first = request();
        create(first, 201);
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM users WHERE login_id = ?", first.get("userId"));
        Map<String, String> second = request();
        second = Map.of(
                "userId", second.get("userId"),
                "password", second.get("password"),
                "passwordConfirm", second.get("passwordConfirm"),
                "email", first.get("email").toLowerCase(Locale.ROOT)
        );
        create(second, 409).andExpect(jsonPath("$.error.fields[0].field").value("email"));
        assertThat(jdbc.queryForMap("SELECT * FROM users WHERE login_id = ?", first.get("userId")))
                .isEqualTo(before);
        assertMissing(second.get("userId"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_roles WHERE user_id = ?",
                Long.class, before.get("user_id"))).isEqualTo(1L);
    }

    @Test
    void mismatchCreatesNoAccount() throws Exception {
        Map<String, String> valid = request();
        Map<String, String> invalid = new java.util.LinkedHashMap<>(valid);
        invalid.put("passwordConfirm", "different");
        create(invalid, 400).andExpect(jsonPath("$.error.fields[0].field").value("passwordConfirm"));
        assertMissing(invalid.get("userId"));
    }

    @Test
    void mixedCaseLettersAndDigitsCommitWithoutRequiringSpecials() throws Exception {
        Map<String, String> valid = new java.util.LinkedHashMap<>(request());
        valid.put("password", "Aa123456");
        valid.put("passwordConfirm", "Aa123456");
        create(valid, 201)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(valid.get("userId")));
        String hash = jdbc.queryForObject("""
                SELECT u.password_hash
                FROM users u
                WHERE u.login_id = ?
                """, String.class, valid.get("userId"));
        assertThat(hash).startsWith("$argon2id$");
        assertThat(new Argon2PasswordEncoder(16, 32, 1, 19456, 2)
                .matches(valid.get("password"), hash)).isTrue();
    }

    @Test
    void lowercaseLettersDigitsAndSpecialsCommitAtMinimumLength() throws Exception {
        Map<String, String> valid = new java.util.LinkedHashMap<>(request());
        valid.put("password", "abcd123!");
        valid.put("passwordConfirm", "abcd123!");
        create(valid, 201)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(valid.get("userId")));
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT u.user_id, u.password_hash
                FROM users u
                WHERE u.login_id = ?
                """, valid.get("userId"));
        String hash = (String) row.get("password_hash");
        assertThat(hash).startsWith("$argon2id$");
        assertThat(new Argon2PasswordEncoder(16, 32, 1, 19456, 2)
                .matches(valid.get("password"), hash)).isTrue();
        assertThat(jdbc.queryForList("""
                SELECT ur.role_code
                FROM user_roles ur
                WHERE ur.user_id = ?
                ORDER BY ur.assignment_id
                """, String.class, row.get("user_id"))).containsExactly("R01");
    }

    @Test
    void realRoleInsertFailureRollsBackUserAndDoesNotLeakSql() throws Exception {
        Map<String, String> request = request();
        // Disposable DB trigger is test-only and rejects just this request's role write.
        String name = "signup_reject_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("""
                CREATE FUNCTION %s() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM users u
                        WHERE u.user_id = NEW.user_id
                            AND u.login_id = '%s'
                    ) THEN
                        RAISE EXCEPTION 'forced role constraint failure' USING ERRCODE = '23514';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """.formatted(name, request.get("userId")));
        try {
            jdbc.execute("CREATE TRIGGER " + name + " BEFORE INSERT ON user_roles "
                    + "FOR EACH ROW EXECUTE FUNCTION " + name + "()");
            create(request, 500);
            assertMissing(request.get("userId"));
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS " + name + " ON user_roles");
            jdbc.execute("DROP FUNCTION IF EXISTS " + name + "()");
        }
    }

    private org.springframework.test.web.servlet.ResultActions create(Map<String, String> request, int code)
            throws Exception {
        return mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request)))
                .andExpect(status().is(code))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(request.get("password")))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("$argon2id$"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("forced role constraint failure"))));
    }

    @Test
    void anonymousAvailabilityQueriesRealMapperAndReservesInactiveAndDeletedIds() throws Exception {
        Map<String, String> request = request();
        String id = request.get("userId");
        long usersBefore = jdbc.queryForObject("SELECT count(*) FROM users", Long.class);
        availability(id, true);
        assertMissing(id);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Long.class)).isEqualTo(usersBefore);

        create(request, 201);
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM users WHERE login_id = ?", id);
        availability(id, false);
        assertThat(jdbc.queryForMap("SELECT * FROM users WHERE login_id = ?", id)).isEqualTo(before);
        for (String accountStatus : List.of("INACTIVE", "DELETED")) {
            jdbc.update("UPDATE users SET status = ? WHERE login_id = ?", accountStatus, id);
            availability(id, false);
        }
        assertThat(jdbc.queryForObject("""
                SELECT count(*)
                FROM user_roles ur
                JOIN users u ON u.user_id = ur.user_id
                WHERE u.login_id = ?
                """, Long.class, id)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*)
                FROM sessions s
                JOIN users u ON u.user_id = s.user_id
                WHERE u.login_id = ?
                """, Long.class, id)).isZero();
    }

    @Test
    void anonymousAvailabilityValidationDoesNotCreateUsersAndProtectedRoutesStayProtected() throws Exception {
        long before = jdbc.queryForObject("SELECT count(*) FROM users", Long.class);
        mvc.perform(get("/api/v1/auth/check-userid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("userId"));
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", "1bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("userId"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Long.class)).isEqualTo(before);
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    private void availability(String userId, boolean available) throws Exception {
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(available))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    private void assertMissing(String id) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE login_id = ?", Long.class, id)).isZero();
    }

    private Map<String, String> request() {
        String id = "signup" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ids.add(id);
        String password = UUID.randomUUID() + "Aa9!";
        return Map.of(
                "userId", id,
                "password", password,
                "passwordConfirm", password,
                "email", id.toUpperCase(Locale.ROOT) + "@EXAMPLE.TEST"
        );
    }
}

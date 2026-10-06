package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import kr.ac.knue.commonfoundation.CommonFoundationApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifies the migrated signup fixtures through PostgreSQL, not migration source-text assertions. */
@SpringBootTest(classes = CommonFoundationApplication.class)
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
class SignupFixtureAcceptanceTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void allThreeFixturesHaveDistinctNormalizedEmailsArgon2idAndOnlyActiveR01() {
        List<Map<String, Object>> accounts = jdbc.queryForList("""
                SELECT u.user_id, u.login_id, u.email, u.password_hash, u.status, u.system_use_yn
                FROM users u
                WHERE u.login_id IN ('testuser1', 'testuser2', 'testuser3')
                ORDER BY u.login_id
                """);
        assertThat(accounts).extracting(row -> row.get("login_id"))
                .containsExactly("testuser1", "testuser2", "testuser3");
        Set<String> emails = new HashSet<>();
        Set<String> hashes = new HashSet<>();
        Set<String> salts = new HashSet<>();
        for (Map<String, Object> account : accounts) {
            String loginId = (String) account.get("login_id");
            assertThat(account.get("status")).isEqualTo("ACTIVE");
            assertThat(account.get("system_use_yn")).isEqualTo("Y");
            String email = (String) account.get("email");
            assertThat(email).isNotNull().isEqualTo(loginId + "@example.com")
                    .isEqualTo(email.toLowerCase(Locale.ROOT));
            assertThat(emails.add(email)).as("fixture emails must be unique").isTrue();

            // Secrets are intentionally unavailable: validate PHC structure, not an invented password.
            String hash = (String) account.get("password_hash");
            assertThat(hash != null && hash.matches(
                    "\\$argon2id\\$v=19\\$m=19456,t=2,p=1\\$[A-Za-z0-9+/]+\\$[A-Za-z0-9+/]+"))
                    .as("fixture stores the agreed Argon2id PHC encoding").isTrue();
            String[] parts = hash.split("\\$");
            assertThat(Base64.getDecoder().decode(parts[4]).length).isEqualTo(16);
            assertThat(Base64.getDecoder().decode(parts[5]).length).isEqualTo(32);
            assertThat(hashes.add(hash)).as("fixture hashes must be distinct").isTrue();
            assertThat(salts.add(parts[4])).as("fixture salts must be distinct").isTrue();

            List<Map<String, Object>> roles = jdbc.queryForList("""
                    SELECT ur.role_code, ur.assignment_type, ur.status, ur.approver_user_id,
                        ur.valid_start_date <= CURRENT_DATE
                            AND (ur.valid_end_date IS NULL OR ur.valid_end_date >= CURRENT_DATE) AS effective
                    FROM user_roles ur
                    WHERE ur.user_id = ?
                    ORDER BY ur.assignment_id
                    """, account.get("user_id"));
            assertThat(roles).hasSize(1);
            assertThat(roles.get(0)).containsEntry("role_code", "R01")
                    .containsEntry("assignment_type", "MANUAL")
                    .containsEntry("status", "ACTIVE")
                    .containsEntry("approver_user_id", null)
                    .containsEntry("effective", true);
        }
    }
}

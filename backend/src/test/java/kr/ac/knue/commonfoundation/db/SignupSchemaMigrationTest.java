package kr.ac.knue.commonfoundation.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Separates migration-resource RED assertions from runner-provided PostgreSQL execution. */
class SignupSchemaMigrationTest {
    @Test
    void forwardMigrationAddsEmailToExistingUsersWithoutAnotherAccountTable() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/migration/V*__*.sql");
        List<String> signupMigrations = new ArrayList<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename != null && filename.matches("V(?:65|66|67)__.*\\.sql")) {
                signupMigrations.add(resource.getContentAsString(StandardCharsets.UTF_8));
            }
        }
        assertThat(signupMigrations)
                .as("T002: signup needs a forward migration in the assigned V65..V67 range")
                .isNotEmpty();
        String sql = String.join("\n", signupMigrations)
                .replaceAll("(?m)--[^\\r\\n]*", "")
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .toLowerCase(Locale.ROOT);
        assertThat(sql).containsPattern("alter\\s+table\\s+(?:if\\s+exists\\s+)?users\\b");
        assertThat(sql).containsPattern("\\bemail\\s+(?:varchar|character\\s+varying)\\s*\\(254\\)");
        assertThat(sql).doesNotContainPattern(
                "create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?(?:sys_user|signup_users)\\b");
        assertThat(sql).doesNotContainPattern("\\bpassword_confirm\\b");
        assertThat(sql).contains("testuser1", "testuser2", "testuser3", "$argon2id$", "'r01'");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
    void postgresUpgradePreservesLegacyAccountsAndMaterializesEmailAndR01Fixtures() throws Exception {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");
        String schema = "signup_schema_" + UUID.randomUUID().toString().replace("-", "");
        Flyway baseline = migration(url, username, password, schema, "64");
        Flyway upgrade = migration(url, username, password, schema, "67");
        try {
            // A fresh, random schema keeps runner/production accounts out of this test.
            baseline.migrate();
            Properties credentials = new Properties();
            if (username != null) {
                credentials.setProperty("user", username);
            }
            if (password != null) {
                credentials.setProperty("password", password);
            }
            try (Connection connection = DriverManager.getConnection(url, credentials)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET search_path TO " + schema);
                }
                Set<String> existingTables = tables(connection, schema);
                long legacyId = insertLegacyAccount(connection);
                upgrade.migrate();
                assertThat(tables(connection, schema))
                        .as("signup must reuse users/user_roles, not introduce another account table")
                        .isEqualTo(existingTables);
                assertEmailColumn(connection, schema);
                assertLegacyAccount(connection, legacyId);
                assertFixtureAccounts(connection);
                assertEmailUniqueness(connection);
            }
        } finally {
            // Flyway owns only this generated schema; never clean a caller-supplied schema.
            upgrade.clean();
        }
    }

    private static Flyway migration(String url, String username, String password, String schema, String target) {
        return Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .target(target)
                .cleanDisabled(false)
                .load();
    }

    private static Set<String> tables(Connection connection, String schema) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT t.table_name
                FROM information_schema.tables t
                WHERE t.table_schema = ?
                    AND t.table_type = 'BASE TABLE'
                """)) {
            statement.setString(1, schema);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tables.add(result.getString(1));
                }
            }
        }
        return tables;
    }

    private static long insertLegacyAccount(Connection connection) throws SQLException {
        // Reuse the existing hash; no plaintext password is embedded or needed for schema verification.
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO users (login_id, password_hash)
                SELECT 'legacyemailtest', u.password_hash
                FROM users u
                ORDER BY u.user_id
                LIMIT 1
                RETURNING user_id
                """)) {
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as("baseline seed account required for legacy upgrade test").isTrue();
                return result.getLong(1);
            }
        }
    }

    private static void assertEmailColumn(Connection connection, String schema) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT c.data_type, c.character_maximum_length
                FROM information_schema.columns c
                WHERE c.table_schema = ?
                    AND c.table_name = 'users'
                    AND c.column_name = 'email'
                """)) {
            statement.setString(1, schema);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as("T002: users.email must exist after the forward migration").isTrue();
                assertThat(result.getString(1)).isEqualTo("character varying");
                assertThat(result.getInt(2)).isEqualTo(254);
            }
        }
    }

    private static void assertLegacyAccount(Connection connection, long legacyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT u.login_id, u.password_hash
                FROM users u
                WHERE u.user_id = ?
                """)) {
            statement.setLong(1, legacyId);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as("legacy account survives upgrade without invented email").isTrue();
                assertThat(result.getString(1)).isEqualTo("legacyemailtest");
                assertThat(result.getString(2)).isNotBlank();
            }
        }
    }

    private static void assertFixtureAccounts(Connection connection) throws SQLException {
        Set<String> emails = new HashSet<>();
        for (String loginId : List.of("testuser1", "testuser2", "testuser3")) {
            long userId;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT u.user_id, u.email, u.password_hash, u.status, u.system_use_yn
                    FROM users u
                    WHERE u.login_id = ?
                    """)) {
                statement.setString(1, loginId);
                try (ResultSet result = statement.executeQuery()) {
                    assertThat(result.next()).as("fixture %s", loginId).isTrue();
                    userId = result.getLong(1);
                    String email = result.getString(2);
                    assertThat(email).as("fixture email for %s", loginId).isNotBlank();
                    assertThat(email).isEqualTo(email.toLowerCase(Locale.ROOT));
                    assertThat(emails.add(email)).as("fixture emails must be unique").isTrue();
                    assertThat(result.getString(3)).startsWith("$argon2id$");
                    assertThat(result.getString(4)).isEqualTo("ACTIVE");
                    assertThat(result.getString(5)).isEqualTo("Y");
                    assertThat(result.next()).isFalse();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT ur.role_code, ur.assignment_type, ur.status
                    FROM user_roles ur
                    WHERE ur.user_id = ?
                    ORDER BY ur.assignment_id
                    """)) {
                statement.setLong(1, userId);
                try (ResultSet result = statement.executeQuery()) {
                    assertThat(result.next()).as("R01 mapping for %s", loginId).isTrue();
                    assertThat(result.getString(1)).isEqualTo("R01");
                    assertThat(result.getString(2)).isEqualTo("MANUAL");
                    assertThat(result.getString(3)).isEqualTo("ACTIVE");
                    assertThat(result.next()).as("signup fixture has no extra role").isFalse();
                }
            }
        }
    }

    private static void assertEmailUniqueness(Connection connection) throws SQLException {
        SQLException duplicate = null;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO users (login_id, password_hash, email)
                SELECT 'duplicateemailtest', u.password_hash, u.email
                FROM users u
                WHERE u.login_id = 'testuser1'
                """)) {
            try {
                statement.executeUpdate();
            } catch (SQLException exception) {
                duplicate = exception;
            }
        }
        assertThat((Throwable) duplicate)
                .as("duplicate email rejected by PostgreSQL, not only by an API pre-check")
                .isNotNull();
        assertThat(duplicate.getSQLState()).isEqualTo("23505");
    }
}

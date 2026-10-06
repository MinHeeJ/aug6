package kr.ac.knue.commonfoundation.common.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Optional PostgreSQL materialization checks. The default no-service test container skips
 * these tests. Explicit runner credentials create an isolated schema and never alter public.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EducationAchievementSchemaIntegrationTest {
    private String url;
    private String username;
    private String password;
    private String schema;
    private Connection connection;

    @BeforeAll
    void migrateAnIsolatedSchemaOnlyWhenRunnerConnectionIsProvided() throws SQLException {
        url = System.getenv("EDUCATION_TEST_DB_URL");
        username = System.getenv("EDUCATION_TEST_DB_USERNAME");
        password = System.getenv("EDUCATION_TEST_DB_PASSWORD");
        assumeTrue(url != null && !url.isBlank()
                && username != null && !username.isBlank() && password != null,
                "Optional PostgreSQL connection settings absent; runner owns SQL verification");
        schema = "education_test_" + UUID.randomUUID().toString().replace("-", "");
        connection = DriverManager.getConnection(url, username, password);
        assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        assertThat(connection.getMetaData().getDatabaseMajorVersion()).isEqualTo(16);
        Flyway.configure()
                .dataSource(url, username, password)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load()
                .migrate();
        connection.setSchema(schema);
        connection.setAutoCommit(false);
    }

    @AfterAll
    void removeOnlyTheUniqueTestSchema() throws SQLException {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        } finally {
            connection.close();
        }
    }

    @Test
    void everyApprovedTableContainsAtLeastThreePersistentExamples() throws SQLException {
        for (String table : List.of(
                "education_achievements",
                "employment_rate_improvement_achievement_details",
                "course_operation_achievement_details",
                "lecture_improvement_achievement_details",
                "employment_rate_batch_jobs",
                "employment_rate_batch_job_items")) {
            assertThat(count("SELECT COUNT(*) FROM " + table)).as(table).isGreaterThanOrEqualTo(3);
        }
        for (EducationAchievementContract.Surface surface : EducationAchievementContract.SURFACES) {
            assertThat(count(
                    "SELECT COUNT(*) FROM education_achievements WHERE achievement_type = ?",
                    surface.achievementType())).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    void duplicateIdentityCannotTurnAnInsertIntoAnUpdate() throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        long before = count("SELECT COUNT(*) FROM education_achievements");
        try {
            SQLException failure = assertThrows(SQLException.class, () -> execute("""
                    INSERT INTO education_achievements (
                        management_no, achievement_type, teacher_user_id, organization_code,
                        evaluation_year, management_item_code, achievement_date, achievement_name
                    )
                    SELECT
                        'DUPLICATE-IDENTITY-TEST', a.achievement_type, a.teacher_user_id, a.organization_code,
                        a.evaluation_year, a.management_item_code, a.achievement_date, a.achievement_name
                    FROM education_achievements a
                    WHERE a.management_no = 'EDU-FR-032-2025-001'
                    """));
            assertThat(failure.getSQLState()).isEqualTo("23505");
        } finally {
            connection.rollback(savepoint);
        }
        assertThat(count("SELECT COUNT(*) FROM education_achievements")).isEqualTo(before);
    }

    @Test
    void invalidStatusAndNegativeCountsAreRejectedWithoutChangingRows() throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        try {
            SQLException failure = assertThrows(SQLException.class, () -> execute("""
                    UPDATE education_achievements
                    SET achievement_status = 'CLIENT_INJECTED'
                    WHERE management_no = 'EDU-FR-029-2025-001'
                    """));
            assertThat(failure.getSQLState()).isEqualTo("23514");
        } finally {
            connection.rollback(savepoint);
        }
        savepoint = connection.setSavepoint();
        try {
            SQLException failure = assertThrows(SQLException.class, () -> execute("""
                    UPDATE employment_rate_batch_jobs
                    SET processed_count = -1
                    WHERE batch_job_id = 'EDU-EMPLOYMENT-JOB-001'
                    """));
            assertThat(failure.getSQLState()).isEqualTo("23514");
        } finally {
            connection.rollback(savepoint);
        }
    }

    @Test
    void menuAndFunctionSeedsPreserveRoleSeparationAndRealTemplateRules() throws SQLException {
        for (EducationAchievementContract.Surface surface : EducationAchievementContract.SURFACES) {
            assertThat(count("""
                    SELECT COUNT(*)
                    FROM menus m
                    JOIN menu_execution_info e
                        ON e.menu_id = m.menu_id
                    WHERE m.screen_id = ? AND m.url = ? AND e.url = m.url
                    """, surface.screenId(), surface.uiRoute())).isEqualTo(1);
            for (String role : List.of("R01", "R02", "R04")) {
                assertThat(count("""
                        SELECT COUNT(*)
                        FROM function_permissions f
                        WHERE f.screen_id = ? AND f.role_code = ?
                          AND f.function_type = 'READ' AND f.permission_allowed = 'ALLOW'
                        """, surface.screenId(), role)).isEqualTo(1);
            }
            assertThat(count("""
                    SELECT COUNT(*)
                    FROM function_permissions f
                    WHERE f.screen_id = ? AND f.role_code = 'R07'
                      AND f.function_type IN ('CREATE', 'UPDATE') AND f.permission_allowed = 'ALLOW'
                    """, surface.screenId())).isZero();
        }
        assertThat(count("""
                SELECT COUNT(*)
                FROM excel_upload_template_rules r
                WHERE r.template_id = ?
                """, EducationAchievementContract.EMPLOYMENT_EXCEL_TEMPLATE_ID)).isEqualTo(5);
        assertThat(count("""
                SELECT COUNT(*)
                FROM employment_rate_batch_jobs j
                WHERE j.batch_job_id LIKE 'EDU-EMPLOYMENT-JOB-%'
                  AND j.processed_count = 0 AND j.target_condition_json ->> 'policyApproved' = 'false'
                """)).isEqualTo(3);
    }

    private long count(String sql, String... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}

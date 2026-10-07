package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Selected-engine read/write checks on an already migrated disposable PostgreSQL database.
 * Follows the existing plain JDBC regression setup, gates before connection, and rolls back every test.
 */
class EducationAchievementFoundationPostgreSqlTest {
    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault(
                "EDUCATION_FOUNDATION_POSTGRES_ENABLED", "false")), "PostgreSQL checks are opt-in");
        String url = System.getenv("SPRING_DATASOURCE_URL");
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");
        Assumptions.assumeTrue(url != null && username != null && password != null,
                "PostgreSQL connection settings are absent; no Spring context or connection is created");
        connection = DriverManager.getConnection(url, username, password);
        connection.setAutoCommit(false);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void everyFeatureHasThreeSeedCasesAndEveryNewTableHasSeedRows() throws Exception {
        for (String type : new String[] {"FR-029", "FR-030", "FR-031", "FR-032"}) {
            try (var statement = connection.prepareStatement("""
                    SELECT COUNT(*)
                    FROM education_achievements
                    WHERE achievement_type = ?
                      AND management_no LIKE 'EDU-SEED-%'
                    """)) {
                statement.setString(1, type);
                try (ResultSet rows = statement.executeQuery()) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getLong(1)).isEqualTo(3L);
                }
            }
        }
        assertThat(count("SELECT COUNT(*) FROM employment_rate_improvement_achievement_details"))
                .isGreaterThanOrEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM course_operation_achievement_details")).isGreaterThanOrEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM lecture_improvement_achievement_details")).isGreaterThanOrEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM employment_rate_batch_jobs WHERE seed_yn = 'Y'"))
                .isGreaterThanOrEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM employment_rate_batch_job_items")).isGreaterThanOrEqualTo(3);
        assertThat(count("""
                SELECT COUNT(*)
                FROM employment_rate_batch_jobs
                WHERE seed_yn = 'Y' AND (status != 'SEED' OR processed_count != 0)
                """)).isZero();
    }

    @Test
    void employmentRateDuplicateIsRejectedButLogicalDeletionReleasesTheNaturalKey() throws Exception {
        String copySeed = """
                INSERT INTO education_achievements (
                    achievement_type, management_no, teacher_user_id, organization_code, evaluation_year,
                    management_item_code, achievement_date, created_by, updated_by
                )
                SELECT
                    a.achievement_type,
                    'education-db-duplicate-test',
                    a.teacher_user_id,
                    a.organization_code,
                    a.evaluation_year,
                    a.management_item_code,
                    a.achievement_date,
                    a.created_by,
                    a.updated_by
                FROM education_achievements a
                WHERE a.management_no = 'EDU-SEED-FR-032-001'
                """;
        rejected(copySeed, "23505");
        execute("""
                UPDATE education_achievements
                SET deleted_yn = 'Y', achievement_status = 'DELETED'
                WHERE management_no = 'EDU-SEED-FR-032-001'
                """);
        assertThat(execute(copySeed)).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(*)
                FROM education_achievements
                WHERE management_no IN ('EDU-SEED-FR-032-001', 'education-db-duplicate-test')
                """)).isEqualTo(2);
    }

    @Test
    void databaseRejectsInvalidDatesSemesterAndOrphanDetailWithoutMutatingSeed() throws Exception {
        rejected("""
                UPDATE employment_rate_improvement_achievement_details
                SET special_lecture_end_date = special_lecture_start_date - 1
                WHERE achievement_id = (
                    SELECT achievement_id
                    FROM education_achievements
                    WHERE management_no = 'EDU-SEED-FR-029-001'
                )
                """, "23514");
        rejected("""
                UPDATE lecture_improvement_achievement_details
                SET semester_code = '3'
                WHERE achievement_id = (
                    SELECT achievement_id
                    FROM education_achievements
                    WHERE management_no = 'EDU-SEED-FR-031-001'
                )
                """, "23514");
        rejected("""
                INSERT INTO course_operation_achievement_details (achievement_id, performance_detail)
                VALUES (-1, 'orphan must not persist')
                """, "23503");
        assertThat(count("""
                SELECT COUNT(*)
                FROM lecture_improvement_achievement_details
                WHERE semester_code IN ('1', '2') AND academic_year = '2025'
                """)).isGreaterThanOrEqualTo(3);
    }

    @Test
    void newHistoryTypesCoexistWithLegacyHistoriesAndRejectUnknownDiscriminator() throws Exception {
        assertThat(count("""
                SELECT COUNT(DISTINCT achievement_type)
                FROM education_achievement_status_histories
                WHERE achievement_type IN ('FR-029', 'FR-030', 'FR-031', 'FR-032')
                """)).isEqualTo(4);
        assertThat(count("""
                SELECT COUNT(DISTINCT achievement_type)
                FROM education_achievement_status_histories
                WHERE achievement_type IN ('LECTURE_EVALUATION', 'LECTURE', 'STUDENT_GUIDANCE', 'DEGREE_COMPLETION')
                """)).isEqualTo(4);
        rejected("""
                INSERT INTO education_achievement_status_histories (
                    achievement_type, achievement_id, next_status, action_type, processed_by
                )
                SELECT
                    'UNKNOWN', a.achievement_id, 'DRAFT', 'CREATE', a.created_by
                FROM education_achievements a
                WHERE a.management_no = 'EDU-SEED-FR-029-001'
                """, "23514");
    }

    @Test
    void rolesAndCanonicalMenuRoutesAndTemplateRulesArePersisted() throws Exception {
        assertThat(count("""
                SELECT COUNT(*)
                FROM menus m
                JOIN menu_permissions p ON p.menu_id = m.menu_id
                WHERE m.url IN (
                    '/faculty/education/employment-rate-improvements',
                    '/faculty/education/course-operations',
                    '/faculty/education/lecture-improvements',
                    '/faculty/education/employment-rate-achievements'
                )
                  AND p.target_type = 'ROLE' AND p.target_id = 'R01' AND p.access_allowed = 'ALLOW'
                """)).isEqualTo(4);
        assertThat(count("""
                SELECT COUNT(*)
                FROM function_permissions
                WHERE screen_id IN (
                    'SCR-EMPLOYMENT-RATE-IMPROVEMENTS', 'SCR-COURSE-OPERATIONS',
                    'SCR-LECTURE-IMPROVEMENTS', 'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS'
                )
                  AND role_code IN ('R02', 'R04', 'R09')
                  AND function_type IN ('CREATE', 'UPDATE') AND permission_allowed = 'ALLOW'
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*)
                FROM excel_upload_templates t
                JOIN excel_upload_template_rules r ON r.template_id = t.template_id
                WHERE t.business_type = 'EMPLOYMENT_RATE_ACHIEVEMENT' AND t.status = 'ACTIVE'
                """)).isEqualTo(5);
    }

    private void rejected(String sql, String sqlState) throws Exception {
        Savepoint savepoint = connection.setSavepoint();
        try {
            assertThatThrownBy(() -> execute(sql))
                    .isInstanceOf(SQLException.class)
                    .extracting(error -> ((SQLException) error).getSQLState())
                    .isEqualTo(sqlState);
        } finally {
            connection.rollback(savepoint);
            connection.releaseSavepoint(savepoint);
        }
    }

    private int execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    private long count(String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            assertThat(rows.next()).isTrue();
            return rows.getLong(1);
        }
    }
}

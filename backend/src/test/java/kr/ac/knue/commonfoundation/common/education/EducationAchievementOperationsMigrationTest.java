package kr.ac.knue.commonfoundation.common.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Classpath contract checks plus opt-in PostgreSQL constraint and transaction checks. */
class EducationAchievementOperationsMigrationTest {
    private final String sql;

    EducationAchievementOperationsMigrationTest() throws Exception {
        sql = new ClassPathResource("db/migration/V65__education_achievement_operations.sql")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void createsOnlyApprovedPhysicalTablesAndTypeSafeDetailRelationships() {
        List<String> tables = List.of(
                "education_achievements",
                "employment_rate_improvement_achievement_details",
                "course_operation_achievement_details",
                "lecture_improvement_achievement_details",
                "employment_rate_batch_jobs",
                "employment_rate_batch_job_items");
        for (String table : tables) {
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS " + table + " (");
            assertThat(sql).contains("COMMENT ON TABLE " + table);
        }
        assertThat(sql.split("CREATE TABLE IF NOT EXISTS ")).hasSize(7);
        assertThat(sql).contains(
                "FOREIGN KEY (achievement_id, achievement_type)",
                "REFERENCES education_achievements(achievement_id, achievement_type)",
                "special_lecture_start_date date",
                "special_lecture_end_date date",
                "mock_exam_question_period text",
                "performance_detail text NOT NULL",
                "performance_content text",
                "academic_year varchar(4)",
                "semester_code varchar(50)");
        assertThat(sql).doesNotContain(
                "CREATE TABLE IF NOT EXISTS employment_rate_bulk_jobs",
                "CREATE TABLE IF NOT EXISTS employment_rate_achievements",
                "CREATE VIEW", "DELETE FROM lecture_achievements");
    }

    @Test
    void preservesLegacyHistoryTypesAndProvidesConcurrentDuplicateAndSoftDeleteConstraints() {
        assertThat(sql).contains(
                "'LECTURE_EVALUATION', 'LECTURE', 'STUDENT_GUIDANCE', 'DEGREE_COMPLETION'",
                "'EMPLOYMENT_RATE_IMPROVEMENT', 'COURSE_OPERATION'",
                "'LECTURE_IMPROVEMENT', 'EMPLOYMENT_RATE_ACHIEVEMENT'",
                "uq_education_achievements_active_identity",
                "achievement_type, teacher_user_id, management_item_code, achievement_date, achievement_name",
                "WHERE deleted_yn = 'N'",
                "(deleted_yn = 'Y') = (achievement_status = 'DELETED')",
                "ADD COLUMN IF NOT EXISTS request_id varchar(100)",
                "total_count = processed_count + unprocessed_count");
    }

    @Test
    void scopesSeedsToRealActorsAndDoesNotInventSemesterCodesOrExecutionPolicy() {
        assertThat(sql).contains(
                "'professor1'", "'professor2'", "'business-owner'",
                "'KNUE-DEPT-COMP'", "'ATTENDANCE'",
                "source.teacher_editable_part", "period.evaluation_year",
                "'employment-rate-seed-normal'", "'employment-rate-seed-boundary'",
                "'employment-rate-seed-delete'", "'EMPLOYMENT_RATE_ACHIEVEMENT'",
                "'employment-rate-achievement-v1'");
        assertThat(sql).doesNotContain("INSERT INTO roles", "INSERT INTO detail_codes");
    }

    @Test
    void menuRegistrationsUseCanonicalRoutesAndDatabaseAllocatedIds() {
        assertThat(sql).contains(
                "'SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT'",
                "'SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT'",
                "'SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT'",
                "'SCR-EMPLOYMENT-RATE-ACHIEVEMENT'",
                "'/faculty/employment-rate-improvement-achievements'",
                "'/faculty/course-offering-operation-achievements'",
                "'/faculty/teaching-improvement-achievements'",
                "'/faculty/employment-rate-achievements'",
                "next_id.max_id + seed.row_no",
                "pg_get_serial_sequence('menus', 'menu_id')",
                "role_seed.role_code <> 'R07'",
                "m.screen_id = 'SCR-EMPLOYMENT-RATE-ACHIEVEMENT'");
        assertThat(sql).doesNotContain("/faculty/education/", "INSERT INTO function_permissions");
    }

    @Test
    void postgresConstraintsAndFullWriteRollbackPreserveExistingRows() throws Exception {
        String url = System.getenv("TEST_POSTGRES_URL");
        assumeTrue(url != null && !url.isBlank(), "TEST_POSTGRES_URL absent: PostgreSQL owned by runner SQL check");
        String username = System.getenv("TEST_POSTGRES_USERNAME");
        String password = System.getenv("TEST_POSTGRES_PASSWORD");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                long baseline = scalar(statement, "SELECT COUNT(*) FROM education_achievements");
                long teacher = scalar(statement, "SELECT user_id FROM users WHERE login_id = 'professor1'");
                String insert = """
                        INSERT INTO education_achievements (
                            achievement_type, teacher_user_id, organization_code,
                            evaluation_year, management_item_code, achievement_date, achievement_name
                        ) VALUES (
                            'COURSE_OPERATION', %d, 'KNUE-DEPT-COMP', '2026',
                            'COURSE_OPERATION', DATE '2026-04-10', 'transaction-constraint-probe'
                        ) RETURNING achievement_id
                        """.formatted(teacher);
                long id = scalar(statement, insert);
                rejects(connection, statement, insert, "23505");
                rejects(connection, statement, """
                        INSERT INTO lecture_improvement_achievement_details (achievement_id)
                        VALUES (%d)
                        """.formatted(id), "23503");
                rejects(connection, statement, """
                        UPDATE education_achievements
                        SET achievement_status = 'UNKNOWN'
                        WHERE achievement_id = %d
                        """.formatted(id), "23514");
                rejects(connection, statement, """
                        UPDATE education_achievements
                        SET deleted_yn = 'Y'
                        WHERE achievement_id = %d
                        """.formatted(id), "23514");
                statement.executeUpdate("""
                        INSERT INTO course_operation_achievement_details (achievement_id, performance_detail)
                        VALUES (%d, '일괄 원자성 검증')
                        """.formatted(id));
                statement.executeUpdate("""
                        INSERT INTO education_achievement_status_histories (
                            achievement_type, achievement_id, next_status, action_type, processed_by, request_id
                        ) VALUES ('COURSE_OPERATION', %d, 'DRAFT', 'CREATE', %d, 'transaction-probe')
                        """.formatted(id, teacher));
                assertThat(scalar(statement, "SELECT COUNT(*) FROM education_achievements"))
                        .isEqualTo(baseline + 1);
                statement.executeUpdate("""
                        UPDATE education_achievements
                        SET deleted_yn = 'Y', achievement_status = 'DELETED'
                        WHERE achievement_id = %d
                        """.formatted(id));
                long replacement = scalar(statement, insert);
                assertThat(replacement).isNotEqualTo(id);
                assertThat(scalar(statement, "SELECT COUNT(*) FROM education_achievements"))
                        .isEqualTo(baseline + 2);
                rejects(connection, statement, """
                        UPDATE employment_rate_batch_jobs
                        SET total_count = -1
                        WHERE batch_job_id = 'employment-rate-seed-normal'
                        """, "23514");
                connection.rollback();
                assertThat(scalar(statement, "SELECT COUNT(*) FROM education_achievements")).isEqualTo(baseline);
                assertThat(scalar(statement, """
                        SELECT COUNT(*) FROM course_operation_achievement_details WHERE achievement_id = %d
                        """.formatted(id))).isZero();
                assertThat(scalar(statement, """
                        SELECT COUNT(*) FROM education_achievement_status_histories
                        WHERE request_id = 'transaction-probe'
                        """)).isZero();
            } finally {
                connection.rollback();
            }
        }
    }

    private long scalar(Statement statement, String query) throws SQLException {
        try (ResultSet rows = statement.executeQuery(query)) {
            assertThat(rows.next()).isTrue();
            return rows.getLong(1);
        }
    }

    private void rejects(Connection connection, Statement statement, String query, String sqlState)
            throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.execute(query))
                    .isInstanceOf(SQLException.class)
                    .extracting(error -> ((SQLException) error).getSQLState())
                    .isEqualTo(sqlState);
        } finally {
            connection.rollback(savepoint);
            connection.releaseSavepoint(savepoint);
        }
    }
}

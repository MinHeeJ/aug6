package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the migrated PostgreSQL storage contract without creating an application context.
 * An isolated database with the full Flyway chain is required; every test rolls back its writes.
 */
class EducationAchievementPersistenceTest {
    private Connection connection;

    @BeforeEach
    void connectToExplicitlyProvidedTestDatabase() throws SQLException {
        String url = System.getenv("EDUCATION_ACHIEVEMENT_TEST_JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "PostgreSQL persistence test database not provided");
        connection = DriverManager.getConnection(
                url,
                System.getenv().getOrDefault("EDUCATION_ACHIEVEMENT_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("EDUCATION_ACHIEVEMENT_TEST_DB_PASSWORD", ""));
        connection.setAutoCommit(false);
    }

    @AfterEach
    void rollbackAndClose() throws SQLException {
        if (connection != null) {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void everyAchievementTypeHasNormalBoundaryAndConfirmedSeedsWithDetails() throws SQLException {
        for (String type : List.of(
                "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION",
                "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT")) {
            assertThat(count("""
                    SELECT COUNT(*)
                    FROM education_achievements a
                    WHERE a.achievement_type = ?
                    """, type)).isGreaterThanOrEqualTo(3);
            assertThat(count("""
                    SELECT COUNT(DISTINCT a.achievement_status)
                    FROM education_achievements a
                    WHERE a.achievement_type = ?
                    """, type)).isGreaterThanOrEqualTo(3);
        }
        assertThat(count("""
                SELECT COUNT(*)
                FROM education_achievements a
                JOIN employment_rate_improvement_achievement_details d ON d.achievement_id = a.achievement_id
                WHERE a.achievement_type = 'EMPLOYMENT_RATE_IMPROVEMENT'
                """)).isGreaterThanOrEqualTo(3);
        assertThat(count("""
                SELECT COUNT(*)
                FROM education_achievements a
                JOIN course_operation_achievement_details d ON d.achievement_id = a.achievement_id
                WHERE a.achievement_type = 'COURSE_OPERATION'
                """)).isGreaterThanOrEqualTo(3);
        assertThat(count("""
                SELECT COUNT(*)
                FROM education_achievements a
                JOIN lecture_improvement_achievement_details d ON d.achievement_id = a.achievement_id
                JOIN detail_codes c ON c.group_id = 'SEMESTER' AND c.code_value = d.semester_code
                WHERE a.achievement_type = 'LECTURE_IMPROVEMENT'
                  AND d.academic_year = '2025'
                  AND c.status = 'ACTIVE'
                  AND c.system_use_yn = 'Y'
                """)).isGreaterThanOrEqualTo(3);
    }

    @Test
    void validCourseCreateAndUpdateReadBackGeneratedIdAndDetail() throws SQLException {
        long id = create("COURSE_OPERATION", null);
        update("""
                INSERT INTO course_operation_achievement_details (achievement_id, performance_detail)
                VALUES (?, '개설 강좌 운영')
                """, id);
        update("""
                UPDATE course_operation_achievement_details
                SET performance_detail = '운영 결과 변경'
                WHERE achievement_id = ?
                """, id);
        assertThat(count("""
                SELECT COUNT(*)
                FROM education_achievements a
                JOIN course_operation_achievement_details d ON d.achievement_id = a.achievement_id
                WHERE a.achievement_id = ?
                  AND a.achievement_status = 'DRAFT'
                  AND a.deleted_yn = 'N'
                  AND d.performance_detail = '운영 결과 변경'
                """, id)).isEqualTo(1);
    }

    @Test
    void employmentNaturalKeyRejectsDuplicateWithoutOverwritingOriginal() throws SQLException {
        String title = "중복 검증 " + UUID.randomUUID();
        long id = create("EMPLOYMENT_RATE_ACHIEVEMENT", title);
        expectSqlState("23505", () -> create("EMPLOYMENT_RATE_ACHIEVEMENT", title));
        assertThat(count("SELECT COUNT(*) FROM education_achievements WHERE achievement_id = ? AND title = ?",
                id, title)).isEqualTo(1);
    }

    @Test
    void softDeletedEmploymentDoesNotBlockNewNaturalIdentity() throws SQLException {
        String title = "삭제후 재등록 " + UUID.randomUUID();
        long deletedId = create("EMPLOYMENT_RATE_ACHIEVEMENT", title);
        update("""
                UPDATE education_achievements
                SET deleted_yn = 'Y', achievement_status = 'DELETED'
                WHERE achievement_id = ?
                """, deletedId);
        long newId = create("EMPLOYMENT_RATE_ACHIEVEMENT", title);
        assertThat(newId).isNotEqualTo(deletedId);
        assertThat(count("SELECT COUNT(*) FROM education_achievements WHERE title = ?", title)).isEqualTo(2);
    }

    @Test
    void parentRejectsUnknownTypeInvalidYearAndEmptyEmploymentTitle() throws SQLException {
        expectSqlState("23514", () -> create("UNKNOWN", null));
        expectSqlState("23514", () -> create("EMPLOYMENT_RATE_ACHIEVEMENT", " "));
        long id = create("COURSE_OPERATION", null);
        expectSqlState("23514", () -> update(
                "UPDATE education_achievements SET evaluation_year = '25' WHERE achievement_id = ?", id));
        expectSqlState("23514", () -> update(
                "UPDATE education_achievements SET achievement_status = 'UNKNOWN' WHERE achievement_id = ?", id));
        expectSqlState("23514", () -> update(
                "UPDATE education_achievements SET deleted_yn = 'Y' WHERE achievement_id = ?", id));
    }

    @Test
    void specialLectureDateOrderIsEnforcedByPostgresql() throws SQLException {
        long id = create("EMPLOYMENT_RATE_IMPROVEMENT", null);
        expectSqlState("23514", () -> update("""
                INSERT INTO employment_rate_improvement_achievement_details (
                    achievement_id, special_lecture_start_date, special_lecture_end_date, mock_exam_question_period
                )
                VALUES (?, DATE '2026-04-11', DATE '2026-04-10', '4월')
                """, id));
        update("""
                INSERT INTO employment_rate_improvement_achievement_details (
                    achievement_id, special_lecture_start_date, special_lecture_end_date, mock_exam_question_period
                )
                VALUES (?, DATE '2026-04-10', DATE '2026-04-10', '4월')
                """, id);
        assertThat(count("""
                SELECT COUNT(*)
                FROM employment_rate_improvement_achievement_details
                WHERE achievement_id = ?
                """, id)).isEqualTo(1);
    }

    @Test
    void detailReferencesRealParentAndRejectsMissingRequiredContent() throws SQLException {
        expectSqlState("23503", () -> update("""
                INSERT INTO course_operation_achievement_details (achievement_id, performance_detail)
                VALUES (-1, '원장 없는 상세')
                """));
        long id = create("COURSE_OPERATION", null);
        expectSqlState("23514", () -> update("""
                INSERT INTO course_operation_achievement_details (achievement_id, performance_detail)
                VALUES (?, ' ')
                """, id));
        long lectureId = create("LECTURE_IMPROVEMENT", null);
        expectSqlState("23514", () -> update("""
                INSERT INTO lecture_improvement_achievement_details (
                    achievement_id, performance_content, academic_year, semester_code
                )
                VALUES (?, '강의 개선', '25', '2025-1')
                """, lectureId));
    }

    @Test
    void parentAndDetailCanBeRolledBackAsOneAtomicUnit() throws SQLException {
        Savepoint before = connection.setSavepoint();
        long id = create("COURSE_OPERATION", null);
        update("""
                INSERT INTO course_operation_achievement_details (achievement_id, performance_detail)
                VALUES (?, '트랜잭션 검증')
                """, id);
        connection.rollback(before);
        assertThat(count("SELECT COUNT(*) FROM education_achievements WHERE achievement_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM course_operation_achievement_details WHERE achievement_id = ?", id))
                .isZero();
    }

    @Test
    void sharedStatusHistoryAcceptsBothLegacyAndNewTypesAndRetainsRequestId() throws SQLException {
        long id = create("COURSE_OPERATION", null);
        for (String type : List.of(
                "LECTURE_EVALUATION", "LECTURE", "STUDENT_GUIDANCE", "DEGREE_COMPLETION",
                "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION",
                "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT")) {
            update("""
                    INSERT INTO education_achievement_status_histories (
                        achievement_type, achievement_id, previous_status, next_status,
                        action_type, processed_by, request_id
                    )
                    SELECT ?, ?, 'DRAFT', 'SUBMITTED', 'SUBMIT', u.user_id, 'persistence-request'
                    FROM users u
                    WHERE u.login_id = 'professor1'
                    """, type, id);
        }
        assertThat(count("""
                SELECT COUNT(*)
                FROM education_achievement_status_histories
                WHERE achievement_id = ? AND request_id = 'persistence-request'
                """, id)).isEqualTo(8);
    }

    @Test
    void batchResultsAreOwnerScopedReadOnlyFixturesWithConsistentCounts() throws SQLException {
        assertThat(count("""
                SELECT COUNT(*)
                FROM employment_rate_batch_jobs j
                JOIN users u ON u.user_id = j.requester_user_id
                WHERE u.login_id = 'business-owner'
                  AND j.target_condition_json @> '{"fixtureOnly":true,"policyApproved":false}'::jsonb
                  AND j.total_count = j.processed_count + j.unprocessed_count
                """)).isGreaterThanOrEqualTo(3);
        assertThat(count("""
                SELECT COUNT(*)
                FROM employment_rate_batch_job_items i
                JOIN employment_rate_batch_jobs j ON j.batch_job_id = i.batch_job_id
                WHERE j.batch_job_id LIKE 'EMPLOYMENT-RESULT-%'
                  AND (i.processed_yn = 'Y' OR length(i.unprocessed_reason) > 0)
                """)).isEqualTo(3);
        expectSqlState("23514", () -> update("""
                UPDATE employment_rate_batch_jobs
                SET total_count = 100
                WHERE batch_job_id = 'EMPLOYMENT-RESULT-001'
                """));
        expectSqlState("23514", () -> update("""
                UPDATE employment_rate_batch_job_items
                SET unprocessed_reason = NULL
                WHERE batch_job_id = 'EMPLOYMENT-RESULT-002'
                """));
    }

    @Test
    void excelDiagnosticsHaveTwoValidOneInvalidAndZeroBusinessWrites() throws SQLException {
        assertThat(count("""
                SELECT COUNT(*)
                FROM excel_upload_histories
                WHERE upload_id = 'EMPLOYMENT-RATE-DIAGNOSTIC-001'
                  AND total_count = 3 AND success_count = 2 AND error_count = 1 AND saved_count = 0
                """)).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(*)
                FROM excel_upload_staging_rows
                WHERE upload_id = 'EMPLOYMENT-RATE-DIAGNOSTIC-001' AND validation_status = 'NORMAL'
                """)).isEqualTo(2);
        assertThat(count("""
                SELECT COUNT(*)
                FROM excel_upload_errors
                WHERE upload_id = 'EMPLOYMENT-RATE-DIAGNOSTIC-001' AND row_number = 3
                """)).isEqualTo(1);
    }

    @Test
    void seededFacultyHasYearScopeManagementItemAndMenuPermissionForFirstCreate() throws SQLException {
        assertThat(count("""
                SELECT COUNT(*)
                FROM users u
                JOIN organization_user_mappings m ON m.user_id = u.user_id
                JOIN input_period_settings p ON p.organization_code = m.organization_code
                JOIN user_roles r ON r.user_id = u.user_id
                WHERE u.login_id = 'professor1'
                  AND m.mapping_type = 'ORGANIZATION' AND m.status = 'ACTIVE'
                  AND r.role_code = 'R01' AND r.status = 'ACTIVE'
                  AND p.evaluation_year = '2026' AND p.area_code = 'EDUCATION' AND p.active_yn = 'Y'
                  AND TIMESTAMP '2026-10-10 12:00:00' BETWEEN p.start_at AND p.end_at
                """)).isGreaterThan(0);
        assertThat(count("""
                SELECT COUNT(*)
                FROM evaluation_management_items m
                JOIN evaluation_elements e ON e.element_id = m.element_id
                JOIN evaluation_items i ON i.item_id = e.item_id
                JOIN evaluation_areas a ON a.area_id = i.area_id
                WHERE a.area_code = 'EDUCATION' AND a.active_yn = 'Y'
                  AND e.evaluation_year = '2026' AND e.active_yn = 'Y'
                  AND m.management_item_code IN (
                      'EMPLOYMENT_RATE_IMPROVEMENT', 'COURSE_OPERATION',
                      'LECTURE_IMPROVEMENT', 'EMPLOYMENT_RATE_ACHIEVEMENT'
                  ) AND m.active_yn = 'Y'
                """)).isGreaterThanOrEqualTo(4);
        assertThat(count("""
                SELECT COUNT(*)
                FROM menus m
                JOIN menu_permissions p ON p.menu_id = m.menu_id
                JOIN function_permissions f ON f.screen_id = m.screen_id
                WHERE m.url LIKE '/faculty/education/%'
                  AND p.target_type = 'ROLE' AND p.target_id = 'R01' AND p.access_allowed = 'ALLOW'
                  AND f.role_code = 'R01' AND f.function_type = 'CREATE' AND f.permission_allowed = 'ALLOW'
                """)).isEqualTo(4);
    }

    private long create(String type, String title) throws SQLException {
        String sql = """
                INSERT INTO education_achievements (
                    achievement_type, management_no, teacher_user_id, organization_code, evaluation_year,
                    management_item_code, achievement_date, title, created_by, updated_by
                )
                SELECT ?, ?, u.user_id, 'KNUE-DEPT-COMP', '2026', ?, DATE '2026-04-10', ?, u.user_id, u.user_id
                FROM users u
                WHERE u.login_id = 'professor1'
                RETURNING achievement_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            statement.setString(2, "TEST-" + UUID.randomUUID());
            statement.setString(3, type);
            statement.setString(4, title);
            try (ResultSet rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                return rows.getLong(1);
            }
        }
    }

    private long count(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                return rows.getLong(1);
            }
        }
    }

    private void update(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private void bind(PreparedStatement statement, Object[] values) throws SQLException {
        for (int index = 0; index < values.length; index++) {
            statement.setObject(index + 1, values[index]);
        }
    }

    private void expectSqlState(String state, SqlAction action) throws SQLException {
        Savepoint before = connection.setSavepoint();
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(action::run)
                    .isInstanceOf(SQLException.class)
                    .satisfies(error -> assertThat(((SQLException) error).getSQLState()).isEqualTo(state));
        } finally {
            connection.rollback(before);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}

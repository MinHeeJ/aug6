package kr.ac.knue.commonfoundation.basic59;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic59DataSeedFoundationContractTest {
    private static final String DATA_SEED_MIGRATION = "db/migration/V57__basic59_data_seed_foundation.sql";

    @Test
    void phase2CreatesAllBasic59DataFoundationTablesForT007ToT010() throws Exception {
        String migration = dataSeedMigration();

        assertThat(migration)
                .contains("CREATE TABLE IF NOT EXISTS evaluation_element_management_item_settings")
                .contains("CREATE TABLE IF NOT EXISTS participation_rate_operation_settings")
                .contains("CREATE TABLE IF NOT EXISTS management_item_score_settings")
                .contains("CREATE TABLE IF NOT EXISTS course_area_group_evaluation_grades")
                .contains("COMMENT ON TABLE evaluation_element_management_item_settings")
                .contains("COMMENT ON TABLE participation_rate_operation_settings")
                .contains("COMMENT ON TABLE management_item_score_settings")
                .contains("COMMENT ON TABLE course_area_group_evaluation_grades")
                .contains("CREATE INDEX IF NOT EXISTS idx_b59_element_management_item_search")
                .contains("CREATE INDEX IF NOT EXISTS idx_b59_participation_rate_search")
                .contains("CREATE INDEX IF NOT EXISTS idx_b59_management_item_score_search")
                .contains("CREATE INDEX IF NOT EXISTS idx_b59_course_area_grade_search");
    }

    @Test
    void phase2SeedsRequiredNormalBoundaryStatusRowsForT007ToT009() throws Exception {
        String migration = dataSeedMigration();

        assertThat(migration)
                .contains("B59-SEED-001 정상 강의평가 관리항목")
                .contains("B59-SEED-001 경계 강의평가 관리항목")
                .contains("B59-SEED-001 상태 차이 강의평가 관리항목")
                .contains("B59-SEED-002 논문 단독저자 배분율")
                .contains("B59-SEED-002 논문 공동저자 2인 배분율")
                .contains("B59-SEED-002 논문 3인 이상 배분율")
                .contains("B59-SEED-003 교육영역 A대학 점수")
                .contains("B59-SEED-003 교육영역 B대학 점수")
                .contains("B59-SEED-003 연구영역 A대학 점수")
                .contains("'B33-DRAFT-2026'")
                .contains("'B33-CONFIRMED-2026'")
                .contains("'Y'")
                .contains("'N'");
    }

    @Test
    void phase2SeedsCourseAreaGradesForR01SelfAndR04FullScopeForT010() throws Exception {
        String migration = dataSeedMigration();

        assertThat(migration)
                .contains("course_area_group_evaluation_grades")
                .contains("'professor2'")
                .contains("'professor1'")
                .contains("'LIBERAL_ARTS'")
                .contains("'MAJOR'")
                .contains("'TEACHING_CERT'")
                .contains("'SEMESTER_1'")
                .contains("'SEMESTER_2'")
                .contains("idx_b59_course_area_grade_scope")
                .contains("teacher_user_id")
                .contains("college_code")
                .contains("department_code");
    }

    @Test
    void phase2StaysInsideDataSeedScopeAndKeepsIdempotentMigrationRules() throws Exception {
        String migration = dataSeedMigration();

        assertThat(migration)
                .doesNotContain("CREATE TABLE IF NOT EXISTS users")
                .doesNotContain("CREATE TABLE IF NOT EXISTS roles")
                .doesNotContain("CREATE TABLE IF NOT EXISTS organizations")
                .doesNotContain("CREATE TABLE IF NOT EXISTS menus")
                .doesNotContain("? IS NULL OR")
                .doesNotContain(":param IS NULL OR")
                .doesNotContain("COALESCE(:param")
                .contains("ON CONFLICT (setting_id) DO UPDATE")
                .contains("ON CONFLICT (grade_id) DO UPDATE")
                .contains("SELECT setval(pg_get_serial_sequence('evaluation_element_management_item_settings', 'setting_id')")
                .contains("SELECT setval(pg_get_serial_sequence('course_area_group_evaluation_grades', 'grade_id')");
    }

    private String dataSeedMigration() throws Exception {
        return new ClassPathResource(DATA_SEED_MIGRATION).getContentAsString(StandardCharsets.UTF_8);
    }
}

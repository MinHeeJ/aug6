package kr.ac.knue.commonfoundation.basic73;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Defines the Phase 1 BASIC-73 Flyway contract before the education-achievement foundation migration exists.
 */
class Basic73EducationAchievementFoundationMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src", "main", "resources", "db", "migration", "V60__basic73_education_achievement_foundation.sql");

    @Test
    void basic73FoundationMigrationCreatesAuditableEducationAchievementLedgerWithCommonStatusLinkage() throws IOException {
        assertThat(Files.exists(MIGRATION))
                .as("BASIC-73 Phase 1 requires its own additive Flyway migration")
                .isTrue();

        String migrationSql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        assertThat(migrationSql)
                .contains("create table if not exists education_achievements")
                .contains("comment on table education_achievements")
                .contains("achievement_type")
                .contains("achievement_status")
                .contains("evaluation_confirmed_yn")
                .contains("deleted_yn")
                .contains("created_at")
                .contains("created_by")
                .contains("updated_at")
                .contains("updated_by")
                .contains("business_status_codes")
                .contains("business_status_transitions")
                .contains("data_change_histories")
                .contains("create index if not exists idx_education_achievements_");
        assertThat(migrationSql)
                .as("the foundation must reuse existing common status and audit infrastructure rather than create a second one")
                .doesNotContain("create table if not exists business_status_codes")
                .doesNotContain("create table if not exists business_status_transitions")
                .doesNotContain("create table if not exists data_change_histories")
                .doesNotContain("create table if not exists roles")
                .doesNotContain("insert into roles");
    }

    @Test
    void basic73FoundationMigrationSeedsThreeLectureEvaluationCasesForNormalBoundaryAndStatusDifferences() throws IOException {
        assertThat(Files.exists(MIGRATION))
                .as("the focused fixture contract requires the BASIC-73 migration")
                .isTrue();

        String migrationSql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        assertThat(migrationSql)
                .contains("insert into education_achievements")
                .contains("b73-seed-001")
                .contains("lecture_evaluation")
                .contains("drafting")
                .contains("submitted")
                .contains("certified");
        assertThat(migrationSql)
                .as("the phase fixture must keep the normal, boundary, and status variants as three distinct rows")
                .contains("drafting")
                .contains("submitted")
                .contains("certified");
    }

    @Test
    void basic73FoundationMigrationAllocatesFourEducationLeafMenusFromTheCurrentMaximumWithoutRoleRedefinition() throws IOException {
        assertThat(Files.exists(MIGRATION))
                .as("the focused menu contract requires the BASIC-73 migration")
                .isTrue();

        String migrationSql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        assertThat(migrationSql)
                .contains("max(menu_id)")
                .contains("scr-lecture-evaluation-achievement")
                .contains("/faculty/education/lecture-evaluation-achievements")
                .contains("scr-lecture-achievement")
                .contains("/faculty/education/lecture-achievements")
                .contains("scr-student-guidance-achievement")
                .contains("/faculty/education/student-guidance-achievements")
                .contains("scr-degree-completion-achievement")
                .contains("/faculty/education/degree-completion-achievements")
                .contains("menu_execution_info")
                .contains("menu_permissions")
                .contains("function_permissions")
                .contains("where not exists")
                .contains("r01")
                .contains("r02")
                .contains("r04");
        assertThat(migrationSql)
                .as("BASIC-73 reuses the existing R01~R09 roles")
                .doesNotContain("create table if not exists roles")
                .doesNotContain("insert into roles");
    }
}

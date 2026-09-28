package kr.ac.knue.commonfoundation.basic73;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Defines the incremental Flyway foundation required before education-achievement APIs are introduced.
 */
class Basic73EducationAchievementFoundationMigrationContractTest {
    @Test
    void educationAchievementFoundationMigrationCreatesTheFiveBusinessTablesAndConnectsExistingStateAndChangeHistoryContracts()
            throws IOException {
        String migrationSql = basic73MigrationSql();

        assertThat(migrationSql)
                .contains("CREATE TABLE IF NOT EXISTS education_achievements")
                .contains("CREATE TABLE IF NOT EXISTS education_achievement_management_values")
                .contains("CREATE TABLE IF NOT EXISTS student_guidance_details")
                .contains("CREATE TABLE IF NOT EXISTS degree_completion_student_details")
                .contains("CREATE TABLE IF NOT EXISTS education_achievement_status_histories")
                .contains("FACULTY_ACHIEVEMENT")
                .contains("business_status_codes")
                .contains("business_status_transitions")
                .contains("data_change_histories")
                .contains("REFERENCES education_achievements")
                .contains("COMMENT ON TABLE education_achievements")
                .contains("COMMENT ON TABLE education_achievement_management_values")
                .contains("COMMENT ON TABLE student_guidance_details")
                .contains("COMMENT ON TABLE degree_completion_student_details")
                .contains("COMMENT ON TABLE education_achievement_status_histories");
    }

    @Test
    void educationAchievementFoundationMigrationSeedsAtLeastThreeNormalBoundaryAndStatusOrPermissionCasesForEveryNewTable()
            throws IOException {
        String migrationSql = basic73MigrationSql();

        assertThat(migrationSql)
                .contains("B73-SEED-001")
                .contains("B73-SEED-002")
                .contains("B73-SEED-003")
                .contains("LECTURE_EVALUATION")
                .contains("LECTURE_ACHIEVEMENT")
                .contains("STUDENT_GUIDANCE")
                .contains("DEGREE_COMPLETION")
                .contains("DRAFTING")
                .contains("SUBMITTED")
                .contains("CERTIFIED")
                .contains("EVALUATION_CONFIRMED")
                .contains("MASTER")
                .contains("DOCTOR")
                .contains("INSERT INTO education_achievements")
                .contains("INSERT INTO education_achievement_management_values")
                .contains("INSERT INTO student_guidance_details")
                .contains("INSERT INTO degree_completion_student_details")
                .contains("INSERT INTO education_achievement_status_histories");
    }

    @Test
    void educationAchievementFoundationMigrationAddsFourLeafMenusUsingIdsAllocatedAfterTheExistingMaximum()
            throws IOException {
        String migrationSql = basic73MigrationSql();

        assertThat(migrationSql)
                .contains("MAX(menu_id)")
                .contains("SCR-LECTURE-EVALUATION-ACHIEVEMENT")
                .contains("/faculty/education/lecture-evaluation-achievements")
                .contains("SCR-LECTURE-ACHIEVEMENT")
                .contains("/faculty/education/lecture-achievements")
                .contains("SCR-STUDENT-GUIDANCE-ACHIEVEMENT")
                .contains("/faculty/education/student-guidance-achievements")
                .contains("SCR-DEGREE-COMPLETION-ACHIEVEMENT")
                .contains("/faculty/education/degree-completion-achievements")
                .contains("INSERT INTO menu_execution_info")
                .contains("INSERT INTO menu_permissions")
                .doesNotContain("ON CONFLICT (menu_id)");
    }

    private String basic73MigrationSql() throws IOException {
        List<Path> migrations;
        try (Stream<Path> paths = Files.list(Path.of("src", "main", "resources", "db", "migration"))) {
            migrations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("V60__basic73_education_achievement_foundation.sql"))
                    .toList();
        }

        assertThat(migrations)
                .as("BASIC-73 requires exactly one incremental Flyway foundation migration")
                .hasSize(1);
        return Files.readString(migrations.get(0), StandardCharsets.UTF_8);
    }
}

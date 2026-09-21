package kr.ac.knue.commonfoundation.basic70;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Keeps BASIC-70 inside the existing runtime boundary while preserving the shared health,
 * session, and increment-only migration contracts required by the final cross-cutting slice.
 */
class Basic70CrossCuttingRegressionContractTest {
    private static final Path APPLICATION = Path.of("src", "main", "resources", "application.yml");
    private static final Path COMPOSE = Path.of("..", "infra", "docker-compose.yml");
    private static final Path FOUNDATION = Path.of(
            "src", "main", "resources", "db", "migration", "V58__basic70_evaluation_setting_foundation.sql");

    @Test
    void existingComposeHealthAndProductionSessionSecurityRemainTheSingleRuntimeContract() throws IOException {
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);
        String application = Files.readString(APPLICATION, StandardCharsets.UTF_8);

        assertThat(compose)
                .contains("services:", "database:", "backend:", "frontend:", "/api/health")
                .doesNotContain("basic70-backend", "basic70-frontend");
        assertThat(application)
                .contains("http-only: true", "on-profile: prod", "secure: true")
                .doesNotContain("basic70.session", "basic70.datasource");
    }

    @Test
    void basic70MigrationOnlyExtendsTheExistingSettingsAndMenuBoundary() throws IOException {
        String migration = Files.readString(FOUNDATION, StandardCharsets.UTF_8).toLowerCase();

        assertThat(migration)
                .contains("insert into menus", "menu_permissions", "evaluation_element_management_item_settings",
                        "participation_allocation_rate_settings", "management_item_evaluation_score_settings")
                .doesNotContain("create table users", "create table roles", "create table organizations",
                        "create table course_area_group_grade_results", "create table achievement_");
    }

    @Test
    void unresolvedAllocationPolicyIsNotMaterializedAsAnInventedDatabaseConstraint() throws IOException {
        String migration = Files.readString(FOUNDATION, StandardCharsets.UTF_8).toLowerCase();

        assertThat(migration)
                .doesNotContain("sum(allocation_rate)")
                .doesNotContain("allocation_rate = 1")
                .doesNotContain("participation completeness");
    }
}

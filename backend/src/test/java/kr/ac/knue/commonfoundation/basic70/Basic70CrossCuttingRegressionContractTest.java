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
    private static final Path BASIC60_CONTROLLER = Path.of(
            "src", "main", "java", "kr", "ac", "knue", "commonfoundation", "basic60", "Basic60Controller.java");

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
    void sharedRuntimeKeepsTheHealthProbePrivacyConfigurationAndSessionBoundaryForBasic70() throws IOException {
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);
        String application = Files.readString(APPLICATION, StandardCharsets.UTF_8);
        String controller = Files.readString(BASIC60_CONTROLLER, StandardCharsets.UTF_8);

        assertThat(compose)
                .contains("SPRING_DATASOURCE_URL", "PRIVACY_CRYPTO_KEY", "PRIVACY_HMAC_KEY",
                        "condition: service_healthy", "http://localhost:8080/api/health")
                .doesNotContain("basic70-database", "5432:5432");
        assertThat(application)
                .contains("maximum-pool-size", "http-only: true", "same-site: lax", "secure: true")
                .doesNotContain("basic70.crypto", "basic70.principal");
        assertThat(controller)
                .contains("requireSettingsAdmin", "currentUser", "R04", "R09", "validatePageSize")
                .doesNotContain("password", "authorization: bearer");
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

    @Test
    void basic70ScopeFenceDoesNotExtendClassificationRulesOrUnrelatedAchievementFlows() throws IOException {
        String migration = Files.readString(FOUNDATION, StandardCharsets.UTF_8).toLowerCase();

        assertThat(migration)
                .doesNotContain("evaluation_areas", "evaluation_items", "evaluation_elements",
                        "evaluation_management_items", "evaluation_score_rules", "participation_rate_rules",
                        "calculation_formula_versions", "course_area_group_grade_results", "achievement_data",
                        "korus", "neis", "external api");
    }

    @Test
    void openQuestionAllocationCompletenessRemainsExplicitlyUnimplementedRatherThanAssumed() throws IOException {
        String controller = Files.readString(BASIC60_CONTROLLER, StandardCharsets.UTF_8).toLowerCase();
        String migration = Files.readString(FOUNDATION, StandardCharsets.UTF_8).toLowerCase();

        assertThat(controller + migration)
                .doesNotContain("allocation total", "allocation completeness", "must equal 100", "sum allocation");
    }
}

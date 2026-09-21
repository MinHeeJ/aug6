package kr.ac.knue.commonfoundation.basic70;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Defines the BASIC-70 Flyway foundation contract before its production migration is added.
 */
class Basic70MenuSeedMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src", "main", "resources", "db", "migration", "V58__basic70_evaluation_setting_foundation.sql");

    @Test
    void basic70FoundationMigrationAllocatesMenuIdsFromTheExistingMaximumAndSeedsAllSettingCases() throws IOException {
        assertThat(Files.exists(MIGRATION))
                .as("BASIC-70 requires its own incremental Flyway migration")
                .isTrue();

        String migrationSql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        assertThat(migrationSql)
                .contains("max(menu_id)")
                .contains("scr-evaluation-element-management-item-settings")
                .contains("scr-participation-allocation-rate-settings")
                .contains("scr-management-item-evaluation-score-settings")
                .contains("b60-seed-001")
                .contains("b60-seed-002")
                .contains("b60-seed-003")
                .contains("evaluation_confirmed_yn")
                .contains("active_yn");
    }
}

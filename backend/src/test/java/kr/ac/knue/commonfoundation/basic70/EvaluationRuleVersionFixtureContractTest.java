package kr.ac.knue.commonfoundation.basic70;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Keeps the reusable evaluation-rule-version lifecycle fixtures available to BASIC-70 guards.
 */
class EvaluationRuleVersionFixtureContractTest {
    private static final Path RULE_VERSION_MIGRATION = Path.of(
            "src", "main", "resources", "db", "migration", "V27__basic33_evaluation_rule_foundation.sql");

    @Test
    void ruleVersionFixtureProvidesDraftConfirmedAndDiscardedVersionsWithEffectivePeriods() throws IOException {
        String migrationSql = Files.readString(RULE_VERSION_MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        assertThat(migrationSql)
                .contains("evaluation_rule_versions")
                .contains("b33-draft-2026")
                .contains("b33-confirmed-2026")
                .contains("b33-discarded-2025")
                .contains("'draft'")
                .contains("'confirmed'")
                .contains("'discarded'")
                .contains("effective_start_date")
                .contains("effective_end_date");
    }
}

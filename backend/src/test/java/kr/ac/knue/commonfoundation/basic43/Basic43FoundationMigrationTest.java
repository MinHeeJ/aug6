package kr.ac.knue.commonfoundation.basic43;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class Basic43FoundationMigrationTest {
    private static final List<String> REQUIRED_TABLES = List.of(
            "department_chair_confirmations",
            "achievement_verifications",
            "academic_grant_approvals",
            "objection_opinions");
    private static final List<String> REQUIRED_HISTORY_METADATA = List.of(
            "change_type",
            "changed_at",
            "changed_by",
            "change_reason",
            "created_at",
            "created_by",
            "updated_at",
            "updated_by");
    private static final Pattern BASIC43_MIGRATION_NAME = Pattern.compile("V44__basic43_.*\\.sql");

    @Test
    void basic43AddsExactlyOneIncrementalMigrationAfterBasic40WithoutRewritingBaselineForReq1280() throws Exception {
        List<String> migrationNames = Arrays.stream(migrationResources())
                .map(Resource::getFilename)
                .sorted()
                .toList();

        assertThat(migrationNames)
                .as("BASIC-43 foundation must be additive after V43 and must not rewrite existing migrations")
                .contains("V43__basic40_exception_period_settings.sql")
                .anySatisfy(name -> assertThat(name).matches(BASIC43_MIGRATION_NAME));
        assertThat(migrationNames)
                .filteredOn(name -> name.toLowerCase().contains("basic43"))
                .hasSize(1)
                .allSatisfy(name -> assertThat(name).matches(BASIC43_MIGRATION_NAME));
    }

    @Test
    void basic43BusinessTablesAndIndexesExistWithHistoryMetadataForReq1336Req1342Req1348Req1354() throws Exception {
        String migration = basic43MigrationSql();

        for (String table : REQUIRED_TABLES) {
            assertThat(migration).contains("CREATE TABLE IF NOT EXISTS " + table);
            assertThat(migration).contains("COMMENT ON TABLE " + table);
            assertThat(migration).contains("CREATE INDEX IF NOT EXISTS idx_" + table + "_");
            for (String column : REQUIRED_HISTORY_METADATA) {
                assertThat(migration)
                        .as(table + " must keep audit/history metadata column " + column)
                        .contains(column);
            }
        }
    }

    @Test
    void basic43TablesKeepAchievementGrantAndObjectionDataLogicallySeparatedForReq1329() throws Exception {
        String migration = basic43MigrationSql();
        Map<String, List<String>> tableSpecificColumns = Map.of(
                "department_chair_confirmations", List.of("achievement_id", "confirm_status", "opinion", "reason_code"),
                "achievement_verifications", List.of("achievement_id", "action_type", "previous_status", "next_status", "evidence_ref"),
                "academic_grant_approvals", List.of("grant_application_id", "approval_status", "payment_amount_snapshot", "account_snapshot_ref"),
                "objection_opinions", List.of("objection_id", "applicant_opinion_snapshot", "reviewer_opinion", "decision_result"));

        tableSpecificColumns.forEach((table, columns) -> columns.forEach(column ->
                assertThat(migration)
                        .as(table + " must contain business-specific column " + column)
                        .contains(column)));
        assertThat(migration).doesNotContain("CREATE TABLE IF NOT EXISTS roles");
        assertThat(migration).doesNotContain("INSERT INTO roles");
    }

    private String basic43MigrationSql() throws Exception {
        Resource[] resources = migrationResources();
        return Arrays.stream(resources)
                .filter(resource -> BASIC43_MIGRATION_NAME.matcher(resource.getFilename()).matches())
                .findFirst()
                .orElseThrow(() -> new AssertionError("BASIC-43 incremental migration is missing"))
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private Resource[] migrationResources() throws Exception {
        return new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql");
    }
}

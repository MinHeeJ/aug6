package kr.ac.knue.commonfoundation.basic46;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class Basic46FoundationMigrationTest {
    private static final Pattern BASIC46_MIGRATION_NAME = Pattern.compile("V4[67]__basic46_.*\\.sql");
    private static final List<String> REQUIRED_TABLES = List.of(
            "evaluation_batch_jobs",
            "evaluation_materials",
            "evaluation_batch_job_items",
            "score_calculation_generations",
            "evaluation_finalizations");
    private static final List<String> REQUIRED_AUDIT_COLUMNS = List.of(
            "request_id",
            "created_at",
            "created_by",
            "updated_at",
            "updated_by");
    private static final List<String> REQUIRED_SCREEN_IDS = List.of(
            "SCR-EVAL-MATERIAL-GENERATION",
            "SCR-EVAL-MATERIAL-DELETION",
            "SCR-SCORE-RECALCULATION",
            "SCR-FINAL-EVAL-CONFIRMATION",
            "SCR-EVAL-BATCH-RESULT");

    @Test
    void basic46ReusesExistingCommonFoundationWithoutRedefiningRolesForReq1363Req1366() throws Exception {
        String baselineSeed = migrationSql("V2__common_foundation_seed.sql");
        String permissionMigration = migrationSql("V5__basic14_permission_operations.sql");
        String batchMigration = migrationSql("V13__basic23_batch_management.sql") + migrationSql("V14__basic23_batch_execution_management.sql");
        String auditMigration = migrationSql("V18__basic29_business_process_logs.sql");
        String basic46 = basic46MigrationSql();

        assertThat(baselineSeed)
                .as("R01~R09 must already be seeded by the common foundation")
                .contains("'R01'")
                .contains("'R02'")
                .contains("'R03'")
                .contains("'R04'")
                .contains("'R05'")
                .contains("'R06'")
                .contains("'R07'")
                .contains("'R08'")
                .contains("'R09'");
        assertThat(permissionMigration).contains("CREATE TABLE IF NOT EXISTS function_permissions");
        assertThat(batchMigration).contains("batch_executions");
        assertThat(auditMigration).contains("business_process_audit_logs");
        assertThat(basic46)
                .doesNotContain("CREATE TABLE IF NOT EXISTS roles")
                .doesNotContain("INSERT INTO roles")
                .contains("'R09'");
    }

    @Test
    void basic46AddsOnlyIncrementalFlywayMigrationForFoundationTablesForReq1473Req1490Req1491() throws Exception {
        List<String> migrationNames = Arrays.stream(migrationResources())
                .map(Resource::getFilename)
                .sorted()
                .toList();

        assertThat(migrationNames)
                .contains("V45__basic43_menu_seed.sql")
                .anySatisfy(name -> assertThat(name).matches(BASIC46_MIGRATION_NAME));
        assertThat(migrationNames)
                .filteredOn(name -> name.toLowerCase().contains("basic46"))
                .hasSize(2)
                .allSatisfy(name -> assertThat(name).matches(BASIC46_MIGRATION_NAME));
    }

    @Test
    void basic46MigrationDefinesRequiredTablesIndexesCommentsAndSeedFixtures() throws Exception {
        String migration = basic46MigrationSql();

        for (String table : REQUIRED_TABLES) {
            assertThat(migration).contains("CREATE TABLE IF NOT EXISTS " + table);
            assertThat(migration).contains("COMMENT ON TABLE " + table);
            assertThat(migration).contains("CREATE INDEX IF NOT EXISTS idx_" + table + "_");
            for (String column : REQUIRED_AUDIT_COLUMNS) {
                assertThat(migration)
                        .as(table + " must keep request/audit metadata column " + column)
                        .contains(column);
            }
        }
        assertThat(migration)
                .contains("B46-SEED-001")
                .contains("B46-SEED-002")
                .contains("B46-SEED-003")
                .contains("batch_type IN ('GENERATION','DELETION','SCORE_RECALCULATION','FINALIZATION','FINALIZATION_CANCEL')")
                .contains("final_status IN ('CERTIFIED','EVALUATION_CONFIRMED','CANCELLED')")
                .contains("source_status IN ('CERTIFIED','EVALUATION_CONFIRMED')")
                .contains("deleted_yn IN ('Y','N')");
    }

    @Test
    void basic46SeedsMenusExecutionInfoAndFunctionPermissionsWithoutNewRoleDefinitions() throws Exception {
        String migration = basic46MigrationSql();

        for (String screenId : REQUIRED_SCREEN_IDS) {
            assertThat(migration).contains(screenId);
        }
        assertThat(migration)
                .contains("INSERT INTO menus")
                .contains("INSERT INTO menu_execution_info")
                .contains("INSERT INTO menu_permissions")
                .contains("INSERT INTO function_permissions")
                .contains("ON CONFLICT (target_type, target_id, menu_id)")
                .contains("ON CONFLICT (screen_id, role_code, function_type)");
    }

    private String basic46MigrationSql() throws Exception {
        return Arrays.stream(migrationResources())
                .filter(resource -> BASIC46_MIGRATION_NAME.matcher(resource.getFilename()).matches())
                .findFirst()
                .orElseThrow(() -> new AssertionError("BASIC-46 incremental migration is missing"))
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private String migrationSql(String filename) throws Exception {
        return Arrays.stream(migrationResources())
                .filter(resource -> filename.equals(resource.getFilename()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(filename + " is missing"))
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private Resource[] migrationResources() throws Exception {
        return new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql");
    }
}

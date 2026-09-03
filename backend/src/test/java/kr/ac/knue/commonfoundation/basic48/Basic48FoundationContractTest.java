package kr.ac.knue.commonfoundation.basic48;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class Basic48FoundationContractTest {
    private static final Pattern BASIC48_FOUNDATION_MIGRATION = Pattern.compile("V48__basic48_.*\\.sql");
    private static final List<String> REQUIRED_GROUP_G_TABLES = List.of(
            "evaluation_materials",
            "score_calculation_generations",
            "evaluation_finalizations",
            "evaluation_batch_jobs");
    private static final List<String> BUSINESS_HISTORY_TABLES = List.of(
            "biz_eval_snapshots",
            "biz_score_calc_hist",
            "biz_score_adj_hist",
            "biz_recalc_hist");
    private static final List<String> SEED_IDS = List.of(
            "B48-SEED-001",
            "B48-SEED-002",
            "B48-SEED-003",
            "B48-SEED-004");

    @Test
    void groupGDataContractsAndCommonPortsExistBeforeBasic48ImplementationForReq1500Req1501Req1502() throws Exception {
        String groupGSql = migrationSql("V46__basic46_evaluation_batch_foundation.sql");

        for (String table : REQUIRED_GROUP_G_TABLES) {
            assertThat(groupGSql)
                    .as("BASIC-48 must reuse the existing Group G table contract: " + table)
                    .contains("CREATE TABLE IF NOT EXISTS " + table)
                    .contains("COMMENT ON TABLE " + table);
        }
        assertThat(classExists("kr.ac.knue.commonfoundation.auth.AuthenticationPort"))
                .as("BASIC-48 must reuse the existing authentication boundary")
                .isTrue();
        assertThat(classExists("kr.ac.knue.commonfoundation.audit.BusinessProcessLogService"))
                .as("BASIC-48 must reuse the existing audit service boundary for read tracking")
                .isTrue();
        assertThat(classExists("kr.ac.knue.commonfoundation.excel.ExcelOperationsService"))
                .as("BASIC-48 must reuse the existing Excel/report service boundary for downloads")
                .isTrue();
    }

    @Test
    void foundationMigrationAddsOnlyBasic48FixtureRegistryAndDoesNotModifyGroupGOrCreateReadModelsForReq1503Req1525Req1526() throws Exception {
        String migration = basic48FoundationMigrationSql();

        assertThat(migration)
                .contains("CREATE TABLE IF NOT EXISTS basic48_seed_fixture_registry")
                .contains("COMMENT ON TABLE basic48_seed_fixture_registry")
                .doesNotContain("CREATE TABLE IF NOT EXISTS roles")
                .doesNotContain("INSERT INTO roles")
                .doesNotContain("ALTER TABLE evaluation_materials")
                .doesNotContain("ALTER TABLE score_calculation_generations")
                .doesNotContain("ALTER TABLE evaluation_finalizations")
                .doesNotContain("ALTER TABLE evaluation_batch_jobs");
        for (String table : BUSINESS_HISTORY_TABLES) {
            assertThat(migration)
                    .as("Phase 1 prepares fixture naming only; " + table + " is created by later user-story phases")
                    .doesNotContain("CREATE TABLE IF NOT EXISTS " + table);
        }
        for (String seedId : SEED_IDS) {
            assertThat(migration).contains(seedId);
        }
    }

    @Test
    void apiTestSupportReusesSessionCookieRolesAndPermissionScopeInputsForReq1504Req1514Req1515Req1517() {
        Cookie cookie = Basic48ApiTestSupport.sessionCookie();
        Basic48ApiTestSupport.Actor admin = Basic48ApiTestSupport.adminR09();
        Basic48ApiTestSupport.Actor teacher = Basic48ApiTestSupport.teacherR01();
        Basic48ApiTestSupport.Actor auditor = Basic48ApiTestSupport.scoreAuditR08();

        assertThat(cookie.getName()).isEqualTo("COMMON_FOUNDATION_SESSION");
        assertThat(admin.currentUser().roles()).containsExactly("R09");
        assertThat(teacher.currentUser().roles()).containsExactly("R01");
        assertThat(auditor.currentUser().roles()).containsExactly("R08");
        assertThat(Basic48ApiTestSupport.allRoleCodes()).containsExactly("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
        assertThat(Basic48ApiTestSupport.readOnlyFunctionTypes()).containsExactly("READ", "DOWNLOAD");
        assertThat(teacher.dataScope()).isEqualTo(Basic48ApiTestSupport.DataScope.SELF);
        assertThat(auditor.dataScope()).isEqualTo(Basic48ApiTestSupport.DataScope.ORGANIZATION);
        assertThat(admin.dataScope()).isEqualTo(Basic48ApiTestSupport.DataScope.ALL);
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private String basic48FoundationMigrationSql() throws Exception {
        return Arrays.stream(migrationResources())
                .filter(resource -> BASIC48_FOUNDATION_MIGRATION.matcher(resource.getFilename()).matches())
                .findFirst()
                .orElseThrow(() -> new AssertionError("BASIC-48 foundation migration is missing"))
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

package kr.ac.knue.commonfoundation.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic37SharedSeedFixtureTest {
    private static final String MIGRATION_PATH = "db/migration/V40__basic37_shared_seed_fixtures.sql";
    private static final String LOOKUP_PROJECTION_MIGRATION_PATH = "db/migration/V41__basic37_researcher_lookup_projection_tables.sql";

    @Test
    void basic37ResearcherLookupProjectionMigrationDeclaresMissingDataModelTables() throws Exception {
        String sql = migrationSql(LOOKUP_PROJECTION_MIGRATION_PATH).toLowerCase();

        assertThat(sql)
                .contains("create table if not exists faculty_search_results")
                .contains("create table if not exists degree_deficiency_targets")
                .contains("faculty_id varchar(100) primary key")
                .contains("target_id varchar(100) primary key")
                .contains("create index if not exists idx_faculty_search_results_keyword")
                .contains("create index if not exists idx_degree_deficiency_targets_profile")
                .contains("insert into faculty_search_results")
                .contains("insert into degree_deficiency_targets")
                .doesNotContain("? is null or")
                .doesNotContain(":param is null or");
    }

    @Test
    void basic37SharedSeedMigrationDeclaresFiveToTenRowsForEachTargetPageFixture() throws Exception {
        String sql = migrationSql();

        assertSeedCount(sql, "BASIC37-SEED-FACULTY-", 5, 10);
        assertSeedCount(sql, "BASIC37-SEED-RESEARCHER-", 5, 10);
        assertSeedCount(sql, "BASIC37-SEED-DEGREE-", 5, 10);
        assertSeedCount(sql, "BASIC37-SEED-BATCH-", 5, 10);
        assertSeedCount(sql, "BASIC37-SEED-EXCEL-TEMPLATE-", 5, 10);
    }

    @Test
    void basic37SharedSeedMigrationPreservesReferenceIntegrityAcrossCoreTables() throws Exception {
        String sql = migrationSql().toLowerCase();

        assertThat(sql)
                .contains("insert into korus_personnel_snapshots")
                .contains("insert into user_roles")
                .contains("insert into organization_user_mappings")
                .contains("insert into researcher_profiles")
                .contains("insert into researcher_degrees")
                .contains("insert into batch_definitions")
                .contains("insert into batch_executions")
                .contains("insert into batch_execution_results")
                .contains("insert into batch_execution_logs")
                .contains("insert into excel_upload_templates")
                .contains("insert into excel_upload_template_rules")
                .contains("insert into excel_upload_template_files")
                .contains("insert into menus")
                .contains("insert into menu_execution_info")
                .contains("insert into menu_permissions")
                .contains("join users admin_user on admin_user.login_id = 'admin'")
                .contains("on conflict");
    }

    private static String migrationSql() throws Exception {
        return migrationSql(MIGRATION_PATH);
    }

    private static String migrationSql(String migrationPath) throws Exception {
        return new ClassPathResource(migrationPath).getContentAsString(StandardCharsets.UTF_8);
    }

    private static void assertSeedCount(String sql, String prefix, int minimum, int maximum) {
        Matcher matcher = Pattern.compile(Pattern.quote(prefix) + "\\d{3}").matcher(sql);
        long count = matcher.results().map(match -> match.group()).distinct().count();
        assertThat(count).as(prefix + " seed count").isBetween((long) minimum, (long) maximum);
    }
}

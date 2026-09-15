package kr.ac.knue.commonfoundation.basic65;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class Basic65TeachingAchievementFoundationMigrationTest {
    private static final Pattern MIGRATION_NAME = Pattern.compile(
            "V57__basic65_teaching_achievement_foundation\\.sql");
    private static final List<String> BUSINESS_TABLES = List.of(
            "teaching_evaluation_achievements",
            "teaching_achievements",
            "student_guidance_achievements",
            "graduate_achievements");
    private static final List<String> STATUS_CODES = List.of(
            "DRAFTING",
            "SUBMITTED",
            "DEPARTMENT_CONFIRMED",
            "DEPARTMENT_REJECTED",
            "CERTIFIED",
            "CERTIFICATION_RETURNED",
            "EVALUATION_CONFIRMED",
            "DELETED");

    @Test
    void basic65AddsOneIncrementalMigrationWithoutRewritingExistingMigrations() throws Exception {
        List<String> migrationNames = Arrays.stream(migrationResources())
                .map(Resource::getFilename)
                .sorted()
                .toList();

        assertThat(migrationNames)
                .contains("V56__basic60_operational_settings_and_grades.sql")
                .anySatisfy(name -> assertThat(name).matches(MIGRATION_NAME));
        assertThat(migrationNames)
                .filteredOn(name -> MIGRATION_NAME.matcher(name).matches())
                .hasSize(1);
    }

    @Test
    void foundationTablesAreAuditableSoftDeletedAndIndexedForTeachingAchievementSearches() throws Exception {
        String migration = migrationSql();

        for (String table : BUSINESS_TABLES) {
            assertThat(migration).contains("CREATE TABLE IF NOT EXISTS " + table);
            assertThat(migration).contains("COMMENT ON TABLE " + table);
            assertThat(migration).contains("deleted_yn char(1) NOT NULL DEFAULT 'N'");
            assertThat(migration).contains("created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP");
            assertThat(migration).contains("created_by bigint");
            assertThat(migration).contains("updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP");
            assertThat(migration).contains("updated_by bigint");
            assertThat(migration).contains("CREATE INDEX IF NOT EXISTS idx_" + table + "_");
        }
    }

    @Test
    void foundationDefinesSharedStatusHistoryAndRequiredDomainConstraints() throws Exception {
        String migration = migrationSql();

        for (String statusCode : STATUS_CODES) {
            assertThat(migration).contains("'" + statusCode + "'");
        }
        assertThat(migration).contains("achievement_status_histories");
        assertThat(migration).contains("from_status");
        assertThat(migration).contains("to_status");
        assertThat(migration).contains("processed_by");
        assertThat(migration).contains("processed_at");
        assertThat(migration).contains("reason");
        assertThat(migration).contains("semester varchar(20) NOT NULL");
        assertThat(migration).contains("academic_year varchar(4) NOT NULL");
        assertThat(migration).contains("guidance_type varchar(50) NOT NULL");
        assertThat(migration).contains("degree_type varchar(20) NOT NULL");
        assertThat(migration).contains("graduate_status varchar(30) NOT NULL");
    }

    @Test
    void migrationSeedsAtLeastThreeRowsForEachBusinessTableUsingExistingUsers() throws Exception {
        String migration = migrationSql();

        for (String table : BUSINESS_TABLES) {
            assertThat(migration).contains("INSERT INTO " + table);
        }
        assertThat(migration).contains("SELECT user_id FROM users ORDER BY user_id LIMIT 1");
        assertThat(migration).doesNotContain("CREATE TABLE IF NOT EXISTS users");
        assertThat(migration).doesNotContain("INSERT INTO roles");
    }

    private String migrationSql() throws Exception {
        return Arrays.stream(migrationResources())
                .filter(resource -> MIGRATION_NAME.matcher(resource.getFilename()).matches())
                .findFirst()
                .orElseThrow(() -> new AssertionError("BASIC-65 incremental migration is missing"))
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private Resource[] migrationResources() throws Exception {
        return new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql");
    }
}

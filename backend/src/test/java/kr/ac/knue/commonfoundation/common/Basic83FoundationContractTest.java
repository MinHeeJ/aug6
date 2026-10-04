package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Verifies the shared BASIC-83 persistence and request-tracing fixtures needed by
 * the independently delivered education-achievement API slices.
 */
class Basic83FoundationContractTest {
    private static final String MIGRATION =
            "db/migration/V65__basic83_education_achievement_late_scope.sql";

    @Test
    void migrationCreatesCanonicalBasic83TablesAndIndexes() throws Exception {
        String sql = migrationSql();

        assertThat(sql).contains(
                "create table if not exists education_achievements",
                "create table if not exists employment_rate_improvement_achievement_details",
                "create table if not exists course_operation_achievement_details",
                "create table if not exists lecture_improvement_achievement_details",
                "create table if not exists employment_rate_batch_jobs",
                "create table if not exists employment_rate_batch_job_items",
                "foreign key (achievement_id) references education_achievements(achievement_id)",
                "foreign key (batch_job_id) references employment_rate_batch_jobs(batch_job_id)",
                "create index if not exists idx_education_achievements_search",
                "create index if not exists idx_employment_rate_batch_jobs_search");
        assertThat(sql).doesNotContain("employment_rate_improvement_achievements");
    }

    @Test
    void migrationSeedsNormalBoundaryAndCertificationFixturesWithImmutableHistories() throws Exception {
        String sql = migrationSql();

        assertThat(sql).contains(
                "b83-eri-001",
                "b83-eri-002",
                "b83-eri-003",
                "'draft'",
                "'submitted'",
                "'certified'",
                "education_achievement_status_histories",
                "data_change_histories");
    }

    @Test
    void migrationConnectsApprovedRolesToTheFourBasic83ScreenMenus() throws Exception {
        String sql = migrationSql();

        assertThat(sql).contains(
                "scr-employment-rate-improvements",
                "scr-course-operations",
                "scr-lecture-improvements",
                "scr-employment-rate-achievements",
                "/faculty/education/employment-rate-improvements",
                "/faculty/education/course-operations",
                "/faculty/education/lecture-improvements",
                "/faculty/education/employment-rate-achievements",
                "('scr-employment-rate-achievements', 'r07')");
    }

    @Test
    void openApiFixtureDeclaresTheBasic83RoutesAndRolesUsedByTheSharedMenuFixtures() throws Exception {
        String contract = new ClassPathResource("contracts/openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();

        assertThat(contract).contains(
                "  /api/business/employment-rate-improvements:",
                "  /api/business/course-operations:",
                "  /api/business/lecture-improvements:",
                "  /api/business/employment-rate-achievements:",
                "x-roles: [r01, r02, r04]",
                "x-roles: [r07]");
    }

    @Test
    void requestIdIsIncludedInTheExistingApiResponseMetaWhenSupplied() {
        ApiResponse<String> response = ApiResponse.ok("fixture", "basic83-request-id");

        assertThat(response.success()).isTrue();
        assertThat(response.meta()).containsEntry("requestId", "basic83-request-id");
    }

    private String migrationSql() throws Exception {
        return new ClassPathResource(MIGRATION)
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();
    }
}

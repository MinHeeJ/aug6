package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Verifies the shared BASIC-83 schema and cross-cutting contract fixtures used by
 * the independently implemented education-achievement resource families.
 */
class Basic83FoundationContractTest {
    @Test
    void migrationProvidesCommonHeadersDetailsBatchTrackingAndSeedCases() throws Exception {
        String migration = resourceText("db/migration/V65__basic83_education_achievement_foundation.sql");

        assertThat(migration).contains(
                "CREATE TABLE IF NOT EXISTS education_achievements",
                "CREATE TABLE IF NOT EXISTS course_operation_achievement_details",
                "CREATE TABLE IF NOT EXISTS lecture_improvement_achievement_details",
                "CREATE TABLE IF NOT EXISTS employment_rate_batch_jobs",
                "achievement_status varchar(30) NOT NULL DEFAULT 'DRAFT'",
                "deleted_yn char(1) NOT NULL DEFAULT 'N'",
                "request_id varchar(100) NOT NULL",
                "EMPLOYMENT_RATE_IMPROVEMENT",
                "COURSE_OPERATION",
                "LECTURE_IMPROVEMENT",
                "B83-ERI-001",
                "B83-ERI-002",
                "B83-ERI-003",
                "B83-ERB-001",
                "B83-ERB-002",
                "B83-ERB-003");
    }

    @Test
    void approvedOpenApiFixtureDeclaresRoleBoundRoutesAndNoUnapprovedBulkExecution() throws Exception {
        String openApi = resourceText("contracts/openapi.yaml");

        assertThat(openApi).contains(
                "/api/business/employment-rate-improvements:",
                "/api/business/course-operations:",
                "/api/business/lecture-improvements:",
                "/api/business/employment-rate-achievements/bulk-jobs:",
                "x-roles: [R01, R02, R04]",
                "x-roles: [R07]",
                "OQ-83-01 확정 전 실행조건과 삭제 허용 상태를 hardcode하지 않는다");
    }

    @Test
    void commonResponseFixturePreservesCallerSuppliedRequestIdForTraceability() {
        ApiResponse<String> response = ApiResponse.ok("fixture", "basic83-request-id");

        assertThat(response.success()).isTrue();
        assertThat(response.meta()).containsEntry("requestId", "basic83-request-id");
        assertThat(response.meta()).containsKeys("timestamp", "traceId");
    }

    private String resourceText(String path) throws Exception {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}

package kr.ac.knue.commonfoundation.basic54;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.Test;

class ReportManagementCommonRulesTest {
    private final ReportManagementMapper mapper = org.mockito.Mockito.mock(ReportManagementMapper.class);
    private final ReportManagementService service = new ReportManagementService(mapper, ReportPolicyProperties.defaults());
    private final CurrentUser r03 = new CurrentUser(3L, "college", "E0003", "단과대학 담당자", List.of("R03"), List.of());
    private final CurrentUser r04 = new CurrentUser(4L, "business", "E0004", "업무 담당자", List.of("R04"), List.of());
    private final CurrentUser r09 = new CurrentUser(9L, "admin", "E0009", "관리자", List.of("R09"), List.of());

    @Test
    void singleOutputUsesFinalSnapshotBaseDateAndApplicableFormVersionAndRecordsSuccessHistory() {
        when(mapper.findReportById("FINAL_EVALUATION", false)).thenReturn(report("FINAL_EVALUATION", "Y"));
        when(mapper.findActivePermissionsForReport(eq("FINAL_EVALUATION"), eq(List.of("R04")), eq(4L)))
                .thenReturn(List.of(permission("R04", "Y", "Y", "ALL")));
        when(mapper.findApplicableFormVersion("FINAL_EVALUATION", LocalDate.parse("2026-01-01")))
                .thenReturn(formVersion("v2.0", LocalDate.parse("2026-01-01")));

        ReportOutputResponse response = service.createReportOutput(
                new ReportOutputRequest("FINAL_EVALUATION", "PDF", LocalDate.parse("2026-01-01"), List.of(101L, 102L), "2026학년도 확정 대상"),
                r04,
                "REQ-B54-SINGLE-OUTPUT");

        assertThat(response.reportId()).isEqualTo("FINAL_EVALUATION");
        assertThat(response.formVersionName()).isEqualTo("v2.0");
        assertThat(response.outputBaseDate()).isEqualTo(LocalDate.parse("2026-01-01"));
        assertThat(response.datasetCode()).isEqualTo("FINAL_EVALUATION_DATASET");
        verify(mapper).insertReportPrintHistory("FINAL_EVALUATION", 4L, "2026학년도 확정 대상", "PDF", 2, "SUCCESS", "reports/generated/FINAL_EVALUATION-v2.0-REQ-B54-SINGLE-OUTPUT.pdf", "REQ-B54-SINGLE-OUTPUT");
    }

    @Test
    void bulkOutputUsesSameBaseDateFormRuleBeforeCreatingQueuedJob() {
        when(mapper.findReportById("FINAL_EVALUATION", false)).thenReturn(report("FINAL_EVALUATION", "Y"));
        when(mapper.findActivePermissionsForReport(eq("FINAL_EVALUATION"), eq(List.of("R03")), eq(3L)))
                .thenReturn(List.of(permission("R03", "Y", "N", "COLLEGE")));
        when(mapper.findApplicableFormVersion("FINAL_EVALUATION", LocalDate.parse("2026-01-01")))
                .thenReturn(formVersion("v2.0", LocalDate.parse("2026-01-01")));
        when(mapper.countRunningBulkReportJobs("FINAL_EVALUATION", "HASH-FINAL-COLLEGE")).thenReturn(0);
        when(mapper.insertBulkReportJob(any(), eq(3L), eq("REQ-B54-BULK-OUTPUT"), eq(2))).thenReturn(job("QUEUED", 0));

        BulkReportJobRow row = service.createBulkReportJob(
                new BulkReportJobCreateRequest("FINAL_EVALUATION", List.of(101L, 102L), "HASH-FINAL-COLLEGE", "PDF", LocalDate.parse("2026-01-01")),
                r03,
                "REQ-B54-BULK-OUTPUT");

        assertThat(row.status()).isEqualTo("QUEUED");
        verify(mapper).findApplicableFormVersion("FINAL_EVALUATION", LocalDate.parse("2026-01-01"));
        verify(mapper).insertBulkReportJobTarget(901L, 101L);
        verify(mapper).insertBulkReportJobTarget(901L, 102L);
    }

    @Test
    void unauthorizedOutputFormatRecordsForbiddenHistoryAndDoesNotCreateFileOrBulkJob() {
        when(mapper.findReportById("FINAL_EVALUATION", false)).thenReturn(report("FINAL_EVALUATION", "Y"));
        when(mapper.findActivePermissionsForReport(eq("FINAL_EVALUATION"), eq(List.of("R03")), eq(3L)))
                .thenReturn(List.of(permission("R03", "Y", "N", "COLLEGE")));

        assertThatThrownBy(() -> service.createReportOutput(
                new ReportOutputRequest("FINAL_EVALUATION", "EXCEL", LocalDate.parse("2026-01-01"), List.of(101L), "권한 밖 Excel"),
                r03,
                "REQ-B54-FORBIDDEN-OUTPUT"))
                .isInstanceOf(ForbiddenException.class);

        verify(mapper).insertReportPrintHistory("FINAL_EVALUATION", 3L, "권한 밖 Excel", "EXCEL", 0, "FORBIDDEN", null, "REQ-B54-FORBIDDEN-OUTPUT");
        verify(mapper, never()).findApplicableFormVersion(any(), any());
        verify(mapper, never()).insertBulkReportJob(any(), any(), any(), eq(1));
    }

    @Test
    void serverValidationRejectsMissingReportRequiredFieldsBeforeAuditMutation() {
        assertThatThrownBy(() -> service.saveReport(
                new ReportSaveRequest(" ", " ", "FACULTY_ACHIEVEMENT", "templates/reports/new.hwp", "DATASET", "Y", " "),
                r09,
                "REQ-B54-VALIDATION"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("보고서 저장 요청");

        verify(mapper, never()).insertReport(any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void seedMigrationUsesThreeCasesConsecutiveMenuIdsAndNoDuplicateBulkColumnRepairStatements() {
        String migration = TestFileReader.read("backend/src/main/resources/db/migration/V55__basic54_report_management_foundation.sql");

        assertThat(migration).contains("'FINAL_EVALUATION'", "'PERSONAL_RESULT_SUMMARY'", "'INACTIVE_TEST_REPORT'");
        assertThat(migration).contains("BASIC-54 정상 보고서 seed", "BASIC-54 경계 보고서 seed", "BASIC-54 미사용 제외 seed");
        assertThat(migration).contains("(560, NULL", "(561, NULL", "(562, NULL", "(563, NULL", "(564, NULL");
        assertThat(migration).contains("ON CONFLICT (menu_id) DO UPDATE");
        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS reports", "CREATE INDEX IF NOT EXISTS idx_reports_search");
        assertThat(migration).contains("total_count INTEGER NOT NULL DEFAULT 0", "request_id VARCHAR(100) NOT NULL", "requested_at TIMESTAMP NOT NULL", "completed_at TIMESTAMP");
        assertThat(migration).contains("target_person_name VARCHAR(100) NOT NULL");
        assertThat(migration).doesNotContain("ALTER TABLE IF EXISTS bulk_report_jobs ADD COLUMN IF NOT EXISTS job_id");
        assertThat(migration).doesNotContain("ALTER TABLE IF EXISTS bulk_report_jobs ADD COLUMN IF NOT EXISTS target_hash");
    }

    @Test
    void dockerPreviewContextsExcludeDependencyAndBuildArtifacts() {
        String rootIgnore = TestFileReader.read(".gitignore");
        String backendDockerIgnore = TestFileReader.read("backend/.dockerignore");
        String frontendDockerIgnore = TestFileReader.read("frontend/.dockerignore");

        assertThat(rootIgnore).contains("node_modules/", ".next/", "dist/", "build/", "coverage/", ".vite/", ".cache/", "*.log", "*.local");
        assertThat(backendDockerIgnore).contains("node_modules", ".next", "dist", "build", "coverage", ".git");
        assertThat(frontendDockerIgnore).contains("node_modules", ".next", "dist", "build", "coverage", ".git");
    }

    @Test
    void openQuestionPolicyValuesAreIsolatedAsConfigurationUntilReviewerDecision() {
        ReportPolicyProperties policies = new ReportPolicyProperties();
        policies.setResultFileRetentionDays(0);
        policies.setPermissionMergeStrategy(ReportPolicyProperties.PermissionMergeStrategy.ANY_ALLOW);
        policies.setUnauthorizedBulkTargetPolicy(ReportPolicyProperties.UnauthorizedBulkTargetPolicy.REJECT_JOB);
        policies.setRecordFailedPrintHistory(false);
        ReportManagementService configuredService = new ReportManagementService(mapper, policies);

        when(mapper.findReportById("FINAL_EVALUATION", false)).thenReturn(report("FINAL_EVALUATION", "Y"));
        when(mapper.findActivePermissionsForReport(eq("FINAL_EVALUATION"), eq(List.of("R03")), eq(3L)))
                .thenReturn(List.of(permission("R03", "Y", "N", "COLLEGE")));

        assertThat(configuredService.reportPolicies().resultFileRetentionDays()).isZero();
        assertThat(configuredService.reportPolicies().permissionMergeStrategy()).isEqualTo(ReportPolicyProperties.PermissionMergeStrategy.ANY_ALLOW);
        assertThat(configuredService.reportPolicies().unauthorizedBulkTargetPolicy()).isEqualTo(ReportPolicyProperties.UnauthorizedBulkTargetPolicy.REJECT_JOB);

        assertThatThrownBy(() -> configuredService.createReportOutput(
                new ReportOutputRequest("FINAL_EVALUATION", "EXCEL", LocalDate.parse("2026-01-01"), List.of(101L), "정책 격리 실패 이력 비활성"),
                r03,
                "REQ-B54-OQ-POLICY"))
                .isInstanceOf(ForbiddenException.class);

        verify(mapper, never()).insertReportPrintHistory("FINAL_EVALUATION", 3L, "정책 격리 실패 이력 비활성", "EXCEL", 0, "FORBIDDEN", null, "REQ-B54-OQ-POLICY");
    }

    private ReportRow report(String reportId, String activeYn) {
        return new ReportRow(reportId, "최종평가서", "FACULTY_ACHIEVEMENT", "templates/reports/final.hwp", "FINAL_EVALUATION_DATASET", activeYn, "저장", 9L, 9L, LocalDateTime.parse("2026-09-08T09:00:00"), LocalDateTime.parse("2026-09-08T09:00:00"));
    }

    private ReportFormVersionRow formVersion(String version, LocalDate effectiveDate) {
        return new ReportFormVersionRow(101L, "FINAL_EVALUATION", "최종평가서", version, effectiveDate, "templates/reports/final.hwp", "Y", "양식 변경", 9L, 9L, LocalDateTime.parse("2026-09-08T09:00:00"), LocalDateTime.parse("2026-09-08T09:00:00"));
    }

    private ReportPermissionRow permission(String roleCode, String allowPrintYn, String allowExcelYn, String dataScope) {
        return new ReportPermissionRow(201L, "ROLE", roleCode, "FINAL_EVALUATION", "최종평가서", "Y", "Y", allowPrintYn, "Y", allowExcelYn, dataScope, "Y", "권한", 9L, 9L, LocalDateTime.parse("2026-09-08T09:00:00"), LocalDateTime.parse("2026-09-08T09:00:00"));
    }

    private BulkReportJobRow job(String status, int progress) {
        return new BulkReportJobRow(901L, "FINAL_EVALUATION", "최종평가서", 3L, "HASH-FINAL-COLLEGE", status, progress, 2, 0, 0, null, "REQ-B54-BULK-OUTPUT", LocalDateTime.parse("2026-09-08T09:00:00"), null);
    }
}

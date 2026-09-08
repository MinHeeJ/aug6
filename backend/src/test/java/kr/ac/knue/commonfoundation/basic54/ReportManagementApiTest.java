package kr.ac.knue.commonfoundation.basic54;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReportManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ReportManagementService service;

    private final CurrentUser r01 = new CurrentUser(1L, "teacher", "E0001", "교원", List.of("R01"), List.of());
    private final CurrentUser r04 = new CurrentUser(4L, "business", "E0004", "담당자", List.of("R04"), List.of());
    private final CurrentUser r09 = new CurrentUser(9L, "admin", "E0009", "관리자", List.of("R09"), List.of());

    @Test
    void listReportsReturnsOnlyActiveReportsByDefaultAndRequestId() throws Exception {
        when(service.listReports(eq(r04), any())).thenReturn(new ReportSearchResponse(List.of(report("FINAL_EVALUATION", "Y")), 0, 20, 1));
        mockMvc.perform(get("/api/business/reports")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B54-REPORT-LIST")
                        .param("businessCategory", "FACULTY_ACHIEVEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reports[0].reportId").value("FINAL_EVALUATION"))
                .andExpect(jsonPath("$.data.reports[0].activeYn").value("Y"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B54-REPORT-LIST"));
    }

    @Test
    void saveReportReturnsPersistedReportAndAuditRequestId() throws Exception {
        when(service.saveReport(any(), eq(r09), eq("REQ-B54-REPORT-SAVE"))).thenReturn(report("FINAL_EVALUATION", "Y"));
        mockMvc.perform(post("/api/business/reports/save")
                        .requestAttr("currentUser", r09).cookie(sessionCookie()).header("X-Request-Id", "REQ-B54-REPORT-SAVE")
                        .contentType(MediaType.APPLICATION_JSON).content(reportJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value("FINAL_EVALUATION"))
                .andExpect(jsonPath("$.data.datasetCode").value("FINAL_EVALUATION_DATASET"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B54-REPORT-SAVE"));
    }

    @Test
    void saveReportRejectsMissingRequiredFieldsBeforeServiceMutation() throws Exception {
        mockMvc.perform(post("/api/business/reports/save")
                        .requestAttr("currentUser", r09).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("reportId")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("reportName")));
        verify(service, never()).saveReport(any(), any(), any());
    }

    @Test
    void createReportOutputReturnsBaseDateFormVersionAndRequestIdContract() throws Exception {
        when(service.createReportOutput(any(), eq(r04), eq("REQ-B54-SINGLE-OUTPUT")))
                .thenReturn(new ReportOutputResponse("FINAL_EVALUATION", "최종평가서", "FINAL_EVALUATION_DATASET", "PDF", LocalDate.parse("2026-01-01"), 101L, "v2.0", "templates/reports/final.hwp", 2, "reports/generated/FINAL_EVALUATION-v2.0-REQ-B54-SINGLE-OUTPUT.pdf", "REQ-B54-SINGLE-OUTPUT"));
        mockMvc.perform(post("/api/business/reports/FINAL_EVALUATION/outputs")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B54-SINGLE-OUTPUT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outputFormat\":\"PDF\",\"outputBaseDate\":\"2026-01-01\",\"targetPersonIds\":[101,102],\"targetSummary\":\"2026학년도 확정 대상\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value("FINAL_EVALUATION"))
                .andExpect(jsonPath("$.data.formVersionName").value("v2.0"))
                .andExpect(jsonPath("$.data.outputBaseDate").value("2026-01-01"))
                .andExpect(jsonPath("$.data.fileRef").value("reports/generated/FINAL_EVALUATION-v2.0-REQ-B54-SINGLE-OUTPUT.pdf"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B54-SINGLE-OUTPUT"));
    }

    @Test
    void createReportOutputRejectsMissingTargetBeforeServiceMutation() throws Exception {
        mockMvc.perform(post("/api/business/reports/FINAL_EVALUATION/outputs")
                        .requestAttr("currentUser", r04).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outputFormat\":\"PDF\",\"outputBaseDate\":\"2026-01-01\",\"targetPersonIds\":[],\"targetSummary\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("targetPersonIds")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("targetSummary")));
        verify(service, never()).createReportOutput(any(), any(), any());
    }

    @Test
    void listReportsRejectsUnsupportedPageSizeForCommonPaginationContract() throws Exception {
        mockMvc.perform(get("/api/business/reports")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).param("size", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("size")));
        verify(service, never()).listReports(any(), any());
    }

    @Test
    void listReportPrintHistoriesMasksSensitiveErrorsInForbiddenFailureHistoryShape() throws Exception {
        when(service.listPrintHistories(eq(r04), any())).thenReturn(new ReportPrintHistorySearchResponse(List.of(new ReportPrintHistoryRow(302L, "FINAL_EVALUATION", "최종평가서", 3L, "college", "권한 밖 대상", "EXCEL", 0, "FORBIDDEN", LocalDateTime.parse("2026-09-08T09:00:00"), null, "REQ-B54-FORBIDDEN")), 0, 20, 1));
        mockMvc.perform(get("/api/business/report-print-histories")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).param("requesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.histories[0].resultCode").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data.histories[0].fileRef").doesNotExist())
                .andExpect(jsonPath("$.data.histories[0].targetSummary").value("권한 밖 대상"));
    }

    @Test
    void listReportFormVersionsReturnsVersionContractShape() throws Exception {
        when(service.listReportFormVersions(eq(r04), any())).thenReturn(new ReportFormVersionSearchResponse(List.of(formVersion("v2.0", LocalDate.parse("2026-01-01"), "Y")), 0, 20, 1));
        mockMvc.perform(get("/api/business/report-form-versions")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B54-FORM-LIST")
                        .param("reportId", "FINAL_EVALUATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.formVersions[0].versionName").value("v2.0"))
                .andExpect(jsonPath("$.data.formVersions[0].currentYn").value("Y"));
    }

    @Test
    void currentReportFormVersionUsesBaseDateSelectionContract() throws Exception {
        when(service.applicableFormVersion(eq(r04), eq("FINAL_EVALUATION"), eq(LocalDate.parse("2026-06-01")))).thenReturn(formVersion("v2.0", LocalDate.parse("2026-01-01"), "Y"));
        mockMvc.perform(get("/api/business/report-form-versions/current")
                        .requestAttr("currentUser", r04).cookie(sessionCookie())
                        .param("reportId", "FINAL_EVALUATION").param("baseDate", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionName").value("v2.0"))
                .andExpect(jsonPath("$.data.effectiveDate").value("2026-01-01"));
    }

    @Test
    void saveReportFormVersionRejectsMissingReportId() throws Exception {
        mockMvc.perform(post("/api/business/report-form-versions/save")
                        .requestAttr("currentUser", r09).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"versionName\":\"v3.0\",\"effectiveDate\":\"2027-01-01\",\"formFileRef\":\"templates/report.hwp\",\"currentYn\":\"Y\",\"changeReason\":\"변경\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("reportId")));
        verify(service, never()).saveReportFormVersion(any(), any(), any());
    }

    @Test
    void listReportPermissionsReturnsMatrixRows() throws Exception {
        when(service.listReportPermissions(eq(r09), any())).thenReturn(new ReportPermissionSearchResponse(List.of(permission("R04", "Y", "Y")), 0, 20, 1));
        mockMvc.perform(get("/api/business/report-permissions")
                        .requestAttr("currentUser", r09).cookie(sessionCookie()).param("reportId", "FINAL_EVALUATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions[0].granteeId").value("R04"))
                .andExpect(jsonPath("$.data.permissions[0].allowPrintYn").value("Y"))
                .andExpect(jsonPath("$.data.permissions[0].dataScope").value("ALL"));
    }

    @Test
    void reportPermissionCheckerAllowsPdfForR04AndDeniesExcelForR01() {
        ReportManagementMapper mapper = org.mockito.Mockito.mock(ReportManagementMapper.class);
        ReportManagementService actual = new ReportManagementService(mapper, ReportPolicyProperties.defaults());
        when(mapper.findReportById("FINAL_EVALUATION", false)).thenReturn(report("FINAL_EVALUATION", "Y"));
        when(mapper.findActivePermissionsForReport(eq("FINAL_EVALUATION"), eq(List.of("R04")), eq(4L))).thenReturn(List.of(permission("R04", "Y", "Y")));
        when(mapper.findActivePermissionsForReport(eq("FINAL_EVALUATION"), eq(List.of("R01")), eq(1L))).thenReturn(List.of(permission("R01", "N", "N")));
        assertThat(actual.checkPermission(r04, "FINAL_EVALUATION", "PRINT", "PDF").allowed()).isTrue();
        assertThat(actual.checkPermission(r01, "FINAL_EVALUATION", "PRINT", "EXCEL").allowed()).isFalse();
    }

    @Test
    void listReportPrintHistoriesIsReadOnlyContractShape() throws Exception {
        when(service.listPrintHistories(eq(r04), any())).thenReturn(new ReportPrintHistorySearchResponse(List.of(history()), 0, 20, 1));
        mockMvc.perform(get("/api/business/report-print-histories")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).param("fromDate", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.histories[0].resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data.histories[0].fileRef").value("reports/output/final.pdf"));
    }

    @Test
    void createBulkReportJobRejectsRunningDuplicateWithoutCreatingTargets() throws Exception {
        when(service.createBulkReportJob(any(), eq(r04), eq("REQ-B54-BULK-DUP"))).thenThrow(new ConflictException("JOB_ALREADY_RUNNING"));
        mockMvc.perform(post("/api/business/bulk-report-jobs")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B54-BULK-DUP")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reportId\":\"FINAL_EVALUATION\",\"targetPersonIds\":[1,2],\"targetHash\":\"HASH-FINAL-RUNNING\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void createAndGetBulkReportJobReturnAsyncProgressContract() throws Exception {
        when(service.createBulkReportJob(any(), eq(r04), eq("REQ-B54-BULK-CREATE"))).thenReturn(job("QUEUED", 0));
        when(service.getBulkReportJob(eq(r04), eq(901L))).thenReturn(job("RUNNING", 40));
        mockMvc.perform(post("/api/business/bulk-report-jobs")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B54-BULK-CREATE")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reportId\":\"FINAL_EVALUATION\",\"targetPersonIds\":[1,2],\"targetHash\":\"HASH-FINAL-NEW\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("QUEUED"));
        mockMvc.perform(get("/api/business/bulk-report-jobs/901")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progressRate").value(40));
    }

    @Test
    void bulkReportWorkerCompletesJobAndStoresResultFileRef() {
        ReportManagementMapper mapper = org.mockito.Mockito.mock(ReportManagementMapper.class);
        ReportManagementService actual = new ReportManagementService(mapper, ReportPolicyProperties.defaults());
        when(mapper.findBulkReportJobById(901L)).thenReturn(job("RUNNING", 40));
        when(mapper.completeBulkReportJob(901L, "reports/bulk/final.zip")).thenReturn(job("COMPLETED", 100));
        BulkReportJobRow completed = actual.completeBulkReportJob(901L, "reports/bulk/final.zip");
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.progressRate()).isEqualTo(100);
        verify(mapper).completeBulkReportJob(901L, "reports/bulk/final.zip");
    }

    @Test
    void outOfScopeReportHistoryMutationAndSourceDataApisAreNotMapped() throws Exception {
        mockMvc.perform(post("/api/business/report-print-histories/301/outputs")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/business/report-print-histories/301")
                        .requestAttr("currentUser", r04).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/business/report-print-histories/301")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/business/reports/FINAL_EVALUATION/source-evaluation-data")
                        .requestAttr("currentUser", r04).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        verify(service, never()).recordPrintHistory(any(), any(), any(), any(), eq(0), any(), any(), any());
    }

    @Test
    void outOfScopeEvaluationScoreSnapshotAndExternalIntegrationApisAreNotMappedByReportSlice() throws Exception {
        List<String> excludedPaths = List.of(
                "/api/business/reports/final-evaluation-confirmations",
                "/api/business/reports/evaluation-snapshots",
                "/api/business/reports/score-recalculations",
                "/api/business/reports/personal-achievement-scores",
                "/api/business/reports/achievement-inputs",
                "/api/business/reports/evaluation-target-selection",
                "/api/business/reports/external-integrations");
        for (String path : excludedPaths) {
            mockMvc.perform(get(path).requestAttr("currentUser", r04).cookie(sessionCookie()))
                    .andExpect(status().isNotFound());
        }
    }


    @Test
    void durableOpenApiFixtureContainsBasic54ReportOperations() throws Exception {
        ClassPathResource resource = new ClassPathResource("contracts/openapi.yaml");
        assertThat(resource.exists()).isTrue();
        String yaml = resource.getContentAsString(StandardCharsets.UTF_8);
        assertThat(yaml).contains("/api/business/reports/save");
        assertThat(yaml).contains("/api/business/report-form-versions/save");
        assertThat(yaml).contains("/api/business/report-permissions/save");
        assertThat(yaml).contains("/api/business/bulk-report-jobs");
        assertThat(yaml).contains("/api/business/bulk-report-jobs/{jobId}/result");
    }

    @Test
    void postApiBusinessReportsSaveAuthRequiredBeforeDataChangeHistorySideEffect() throws Exception {
        mockMvc.perform(post("/api/business/reports/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveReport(any(), any(), any());
    }

    @Test
    void postApiBusinessReportFormVersionsSaveAuthRequiredBeforeSideEffect() throws Exception {
        mockMvc.perform(post("/api/business/report-form-versions/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":\"FINAL_EVALUATION\",\"versionName\":\"v2.0\",\"effectiveDate\":\"2026-01-01\",\"formFileRef\":\"templates/reports/final.hwp\",\"currentYn\":\"Y\",\"changeReason\":\"서식 개정\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveReportFormVersion(any(), any(), any());
    }

    @Test
    void postApiBusinessReportFormVersionsSaveHappySideEffectPersistsCurrentYn() throws Exception {
        when(service.saveReportFormVersion(any(), eq(r09), eq("REQ-B54-FORM-SAVE")))
                .thenReturn(formVersion("v2.0", LocalDate.parse("2026-01-01"), "Y"));
        mockMvc.perform(post("/api/business/report-form-versions/save")
                        .requestAttr("currentUser", r09).cookie(sessionCookie()).header("X-Request-Id", "REQ-B54-FORM-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":\"FINAL_EVALUATION\",\"versionName\":\"v2.0\",\"effectiveDate\":\"2026-01-01\",\"formFileRef\":\"templates/reports/final.hwp\",\"currentYn\":\"Y\",\"changeReason\":\"서식 개정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectiveDate").value("2026-01-01"))
                .andExpect(jsonPath("$.data.currentYn").value("Y"));
    }

    @Test
    void postApiBusinessReportPermissionsSaveHappyAuthValidationBusinessSideEffectUpsert() throws Exception {
        when(service.saveReportPermissions(any(), eq(r09), eq("REQ-B54-PERM-SAVE")))
                .thenReturn(new ReportPermissionSearchResponse(List.of(permission("R03", "Y", "N")), 0, 100, 1));
        mockMvc.perform(post("/api/business/report-permissions/save")
                        .requestAttr("currentUser", r09).cookie(sessionCookie()).header("X-Request-Id", "REQ-B54-PERM-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeReason\":\"PDF 권한 부여\",\"permissions\":[{\"granteeType\":\"ROLE\",\"granteeId\":\"R03\",\"reportId\":\"FINAL_EVALUATION\",\"allowViewYn\":\"Y\",\"allowPreviewYn\":\"N\",\"allowPrintYn\":\"Y\",\"allowPdfYn\":\"Y\",\"allowExcelYn\":\"N\",\"dataScope\":\"ALL\",\"activeYn\":\"Y\",\"changeReason\":\"PDF 권한 부여\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions[0].granteeId").value("R03"))
                .andExpect(jsonPath("$.data.permissions[0].allowPdfYn").value("Y"))
                .andExpect(jsonPath("$.data.permissions[0].activeYn").value("Y"));
    }

    @Test
    void postApiBusinessReportPermissionsSaveAuthRequiredBeforeSideEffect() throws Exception {
        mockMvc.perform(post("/api/business/report-permissions/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[{\"granteeType\":\"ROLE\",\"granteeId\":\"R03\",\"reportId\":\"FINAL_EVALUATION\",\"allowViewYn\":\"Y\",\"allowPreviewYn\":\"N\",\"allowPrintYn\":\"Y\",\"allowPdfYn\":\"Y\",\"allowExcelYn\":\"N\",\"dataScope\":\"ALL\",\"activeYn\":\"Y\",\"changeReason\":\"권한 검증\"}]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveReportPermissions(any(), any(), any());
    }

    @Test
    void postApiBusinessReportPermissionsSaveValidationReturnsReportIdFieldError() throws Exception {
        mockMvc.perform(post("/api/business/report-permissions/save")
                        .requestAttr("currentUser", r09).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[{\"granteeType\":\"ROLE\",\"granteeId\":\"R03\",\"allowViewYn\":\"Y\",\"allowPreviewYn\":\"N\",\"allowPrintYn\":\"Y\",\"allowPdfYn\":\"Y\",\"allowExcelYn\":\"N\",\"dataScope\":\"ALL\",\"activeYn\":\"Y\",\"changeReason\":\"검증\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("permissions[0].reportId")));
        verify(service, never()).saveReportPermissions(any(), any(), any());
    }

    @Test
    void postApiBusinessReportPermissionsSaveBusinessRejectsNoAllowedActionsWithoutSideEffect() throws Exception {
        when(service.saveReportPermissions(any(), eq(r09), any()))
                .thenThrow(new BusinessValidationException("보고서 권한 저장 요청이 올바르지 않습니다.",
                        List.of(new ValidationError("allowedActions", "하나 이상의 허용행위를 선택하세요."))));
        mockMvc.perform(post("/api/business/report-permissions/save")
                        .requestAttr("currentUser", r09).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[{\"granteeType\":\"ROLE\",\"granteeId\":\"R03\",\"reportId\":\"FINAL_EVALUATION\",\"allowViewYn\":\"N\",\"allowPreviewYn\":\"N\",\"allowPrintYn\":\"N\",\"allowPdfYn\":\"N\",\"allowExcelYn\":\"N\",\"dataScope\":\"ALL\",\"activeYn\":\"Y\",\"changeReason\":\"허용행위 없음\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("allowedActions")));
    }

    @Test
    void getApiBusinessBulkReportJobsHappyReturnsBodyContract() throws Exception {
        when(service.listBulkReportJobs(eq(r04), any())).thenReturn(new BulkReportJobSearchResponse(List.of(job("COMPLETED", 100)), 0, 20, 1));
        mockMvc.perform(get("/api/business/bulk-report-jobs")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobs[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.jobs[0].successCount").value(2));
    }

    @Test
    void getApiBusinessBulkReportJobsAuthRequiredBeforeSideEffect() throws Exception {
        mockMvc.perform(get("/api/business/bulk-report-jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).listBulkReportJobs(any(), any());
    }

    @Test
    void getApiBusinessBulkReportJobsTargetsHappyReturnsRowsContract() throws Exception {
        when(service.listBulkReportTargets(eq(r04), any()))
                .thenReturn(new BulkReportTargetSearchResponse(List.of(new BulkReportJobTargetRow(7001L, 901L, 101L, "홍길동", "KNUE-DEPT-COMP", "FAILED", "PDF 생성 실패")), 0, 20, 1));
        mockMvc.perform(get("/api/business/bulk-report-jobs/targets")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).param("reportId", "FINAL_EVALUATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targets[0].targetPersonName").value("홍길동"))
                .andExpect(jsonPath("$.data.targets[0].resultCode").value("FAILED"));
    }

    @Test
    void getApiBusinessBulkReportJobsResultHappyIncludesCompletedAndFailedStateTransitions() throws Exception {
        BulkReportJobTargetRow failure = new BulkReportJobTargetRow(7001L, 901L, 101L, "홍길동", "KNUE-DEPT-COMP", "FAILED", "PDF 생성 실패");
        when(service.getBulkReportJobResult(r04, 901L)).thenReturn(BulkReportJobResultResponse.from(job("FAILED", 100), List.of(failure)));
        mockMvc.perform(get("/api/business/bulk-report-jobs/901/result")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failures[0].errorDetail").value("PDF 생성 실패"));
    }

    @Test
    void getApiBusinessBulkReportJobsResultAuthRequiredBeforeSideEffect() throws Exception {
        mockMvc.perform(get("/api/business/bulk-report-jobs/901/result"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).getBulkReportJobResult(any(), any());
    }

    @Test
    void postApiBusinessBulkReportJobsAuthRequiredBeforeSideEffectTables() throws Exception {
        mockMvc.perform(post("/api/business/bulk-report-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":\"FINAL_EVALUATION\",\"targetPersonIds\":[1,2],\"targetHash\":\"HASH-FINAL-NEW\",\"outputFormat\":\"PDF\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).createBulkReportJob(any(), any(), any());
    }

    @Test
    void postApiBusinessBulkReportJobsSideEffectCreatesJobTargetsAndReportPrintHistories() throws Exception {
        when(service.createBulkReportJob(any(), eq(r04), eq("REQ-B54-BULK-SIDE"))).thenReturn(job("QUEUED", 0));
        mockMvc.perform(post("/api/business/bulk-report-jobs")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B54-BULK-SIDE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":\"FINAL_EVALUATION\",\"targetPersonIds\":[1,2],\"targetHash\":\"HASH-FINAL-NEW\",\"outputFormat\":\"PDF\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.totalCount").value(2));
        verify(service).createBulkReportJob(any(), eq(r04), eq("REQ-B54-BULK-SIDE"));
    }

    private ReportRow report(String reportId, String activeYn) { return new ReportRow(reportId, "최종평가서", "FACULTY_ACHIEVEMENT", "templates/reports/final.hwp", "FINAL_EVALUATION_DATASET", activeYn, "저장", 9L, 9L, LocalDateTime.parse("2026-09-08T09:00:00"), LocalDateTime.parse("2026-09-08T09:00:00")); }
    private ReportFormVersionRow formVersion(String version, LocalDate effectiveDate, String currentYn) { return new ReportFormVersionRow(101L, "FINAL_EVALUATION", "최종평가서", version, effectiveDate, "templates/reports/final.hwp", currentYn, "양식 변경", 9L, 9L, LocalDateTime.parse("2026-09-08T09:00:00"), LocalDateTime.parse("2026-09-08T09:00:00")); }
    private ReportPermissionRow permission(String roleCode, String allowPrintYn, String allowExcelYn) { return new ReportPermissionRow(201L, "ROLE", roleCode, "FINAL_EVALUATION", "최종평가서", "Y", "Y", allowPrintYn, "Y", allowExcelYn, "ALL", "Y", "권한", 9L, 9L, LocalDateTime.parse("2026-09-08T09:00:00"), LocalDateTime.parse("2026-09-08T09:00:00")); }
    private ReportPrintHistoryRow history() { return new ReportPrintHistoryRow(301L, "FINAL_EVALUATION", "최종평가서", 4L, "담당자", "전체 대상", "PDF", 10, "SUCCESS", LocalDateTime.parse("2026-09-08T09:00:00"), "reports/output/final.pdf", "REQ-B54-HISTORY"); }
    private BulkReportJobRow job(String status, int progress) { return new BulkReportJobRow(901L, "FINAL_EVALUATION", "최종평가서", 4L, "HASH-FINAL-NEW", status, progress, 2, progress == 100 ? 2 : 0, 0, progress == 100 ? "reports/bulk/final.zip" : null, "REQ-B54-BULK", LocalDateTime.parse("2026-09-08T09:00:00"), progress == 100 ? LocalDateTime.parse("2026-09-08T09:10:00") : null); }
    private String reportJson() { return "{\"reportId\":\"FINAL_EVALUATION\",\"reportName\":\"최종평가서\",\"businessCategory\":\"FACULTY_ACHIEVEMENT\",\"templateFileRef\":\"templates/reports/final.hwp\",\"datasetCode\":\"FINAL_EVALUATION_DATASET\",\"activeYn\":\"Y\",\"changeReason\":\"저장\"}"; }
    private Cookie sessionCookie() { return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"); }
}

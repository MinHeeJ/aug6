package kr.ac.knue.commonfoundation.taskgaps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateAchievementController;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateAchievementService;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateBulkJobRow;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers the R07-only BASIC-83 batch-job HTTP boundary, including the persisted
 * result shape that distinguishes completed and unprocessed targets.
 */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateBulkJobContractTest {
    private static final CurrentUser R01 = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());
    private static final CurrentUser R07 = new CurrentUser(
            107L,
            "excel-operator",
            "E0107",
            "엑셀담당자",
            List.of("R07"),
            List.of());

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateAchievementService service;

    @Test
    void approvedOpenApiFixtureDeclaresR07BulkJobSubmissionAndResultOperations() throws Exception {
        String openApi = new ClassPathResource("contracts/openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("operationId: createEmploymentRateBulkJob")
                .contains("operationId: getEmploymentRateBulkJob")
                .contains("x-roles: [R07]")
                .contains("employment_rate_bulk_jobs와 employment_rate_bulk_job_items 기록");
    }

    @Test
    void r07UploadUsesTheEmploymentRateTemplateBoundary() throws Exception {
        MockMultipartFile spreadsheet = new MockMultipartFile(
                "file",
                "employment-rate.csv",
                "text/csv",
                "교번,관리항목코드,업적발생일,실적명,첨부참조\nE0107,EMPLOYMENT_RATE,2026-10-04,취업률 실적,\n"
                        .getBytes(StandardCharsets.UTF_8));
        when(service.upload(any(), eq(R07))).thenReturn(new ExcelUploadResult(
                "UP-ERA-001",
                "EMPLOYMENT_RATE_ACHIEVEMENT",
                "employment-rate.csv",
                "VALIDATED",
                1,
                1,
                0,
                0,
                0,
                List.of()));

        mockMvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(spreadsheet)
                        .requestAttr("currentUser", R07)
                        .header("X-Request-Id", "REQ-B83-EXCEL-UPLOAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.businessType").value("EMPLOYMENT_RATE_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.errorCount").value(0))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-EXCEL-UPLOAD"));

        verify(service).upload(any(), eq(R07));
    }

    @Test
    void r07CanSubmitApprovedBatchJobAndReceivesAcceptedJobId() throws Exception {
        EmploymentRateBulkJobRow job = batchJob();
        when(service.requestBatchJob(any(), eq(R07))).thenReturn(job);

        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .requestAttr("currentUser", R07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "REQ-B83-BATCH-SUBMIT")
                        .content("""
                                {
                                  "evaluationYear":"2026",
                                  "actionType":"GENERATE",
                                  "targetCondition":{"organizationCode":"KNUE-DEPT-COMP"}
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchJobId").value("ERB-2026-001"))
                .andExpect(jsonPath("$.data.actionType").value("GENERATE"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-BATCH-SUBMIT"));

        verify(service).requestBatchJob(any(), eq(R07));
    }

    @Test
    void r01CannotSubmitR07OnlyBatchJob() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .requestAttr("currentUser", R01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\"" + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).requestBatchJob(any(), any());
    }

    @Test
    void r07CanReadPerTargetProcessingResultIncludingUnprocessedReason() throws Exception {
        when(service.getBatchJob("ERB-2026-001", R07)).thenReturn(batchJob());

        mockMvc.perform(get("/api/business/employment-rate-achievements/bulk-jobs/ERB-2026-001")
                        .requestAttr("currentUser", R07)
                        .header("X-Request-Id", "REQ-B83-BATCH-RESULT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.excludedCount").value(1))
                .andExpect(jsonPath("$.data.items[0].processed").value(true))
                .andExpect(jsonPath("$.data.items[1].processed").value(false))
                .andExpect(jsonPath("$.data.items[1].unprocessedReason").value("대상 조건 미충족"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-BATCH-RESULT"));

        verify(service).getBatchJob("ERB-2026-001", R07);
    }

    private EmploymentRateBulkJobRow batchJob() {
        return new EmploymentRateBulkJobRow(
                "ERB-2026-001",
                "2026",
                Map.of("organizationCode", "KNUE-DEPT-COMP"),
                "GENERATE",
                "COMPLETED",
                2,
                1,
                0,
                1,
                LocalDateTime.parse("2026-10-04T09:00:00"),
                List.of(
                        new EmploymentRateBulkJobRow.EmploymentRateBulkJobItem(101L, true, null),
                        new EmploymentRateBulkJobRow.EmploymentRateBulkJobItem(102L, false, "대상 조건 미충족")));
    }
}

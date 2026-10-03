package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorRow;
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

/** Contract tests for the controller that owns employment-rate export and R07 operations. */
@WebMvcTest(EmploymentRateAchievementOperationsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateAchievementOperationsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateAchievementOperationsService service;

    private final CurrentUser r01 = user(101L, "R01");
    private final CurrentUser r07 = user(107L, "R07");

    @Test
    void downloadReturnsAnEmploymentRateWorkbookAndForwardsBothPaginationParameters() throws Exception {
        when(service.download(1, 50, r01)).thenReturn(new byte[] {0x50, 0x4b});

        mockMvc.perform(get("/api/business/employment-rate-achievements/download")
                        .param("page", "1")
                        .param("pageSize", "50")
                        .header("X-Request-Id", "download-83")
                        .requestAttr("currentUser", r01))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("X-Request-Id", "download-83"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(
                        "employment-rate-achievements.xlsx")));

        verify(service).download(1, 50, r01);
    }

    @Test
    void uploadAsR07ReturnsTheSharedValidationResult() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employment-rate.csv",
                "text/csv",
                "templateVersion,managementItemCode\nv1.0,EMPLOYMENT_RATE\n".getBytes());
        when(service.upload(any(), eq(r07))).thenReturn(uploadResult());

        mockMvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(file)
                        .requestAttr("currentUser", r07))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadId").value("UP-83"))
                .andExpect(jsonPath("$.data.successCount").value(1));

        verify(service).upload(any(), eq(r07));
    }

    @Test
    void uploadRejectsANonR07CallerBeforeTheService() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employment-rate.csv",
                "text/csv",
                "header\n".getBytes());

        mockMvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(file)
                        .requestAttr("currentUser", r01))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).upload(any(), any());
    }

    @Test
    void bulkJobReturnsConflictWithoutCreatingAnUnapprovedJob() throws Exception {
        EmploymentRateBulkJobRequest request = new EmploymentRateBulkJobRequest(
                "2026",
                "GENERATE",
                java.util.Map.of("organizationCode", "ORG-1"));
        when(service.createBulkJob(eq(request), eq(r07))).thenThrow(new ConflictException("정책 미확정"));

        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evaluationYear":"2026","actionType":"GENERATE",
                                 "targetCondition":{"organizationCode":"ORG-1"}}
                                """)
                        .requestAttr("currentUser", r07))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));

        verify(service).createBulkJob(request, r07);
    }

    @Test
    void getBulkJobReturnsTheRecordedR07Result() throws Exception {
        EmploymentRateBulkJobResult result = new EmploymentRateBulkJobResult(
                "JOB-83",
                "2026",
                "GENERATE",
                "COMPLETED",
                2,
                1,
                1);
        when(service.getBulkJob("JOB-83", r07)).thenReturn(result);

        mockMvc.perform(get("/api/business/employment-rate-achievements/bulk-jobs/{jobId}", "JOB-83")
                        .requestAttr("currentUser", r07))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value("JOB-83"))
                .andExpect(jsonPath("$.data.unprocessedCount").value(1));

        verify(service).getBulkJob("JOB-83", r07);
    }

    @Test
    void materializedOpenApiFixtureIsAvailableOnTheBackendTestClasspath() {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");

        org.assertj.core.api.Assertions.assertThat(contract.exists()).isTrue();
    }

    private EmploymentRateExcelUploadResult uploadResult() {
        return new EmploymentRateExcelUploadResult(
                "UP-83",
                "employment-rate.csv",
                1,
                1,
                0,
                List.<ExcelUploadErrorRow>of());
    }

    private CurrentUser user(Long userId, String role) {
        return new CurrentUser(
                userId,
                "user" + userId,
                "E" + userId,
                "사용자",
                List.of(role),
                List.of());
    }
}

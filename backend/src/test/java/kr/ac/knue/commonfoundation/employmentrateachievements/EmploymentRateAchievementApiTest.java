package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
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
import org.springframework.util.StreamUtils;

/** MockMvc contract tests for the controller that owns all employment-rate routes. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateAchievementService service;

    private final CurrentUser r01 = new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(107L, "excel", "E0107", "엑셀담당자", List.of("R07"), List.of());

    @Test
    void approvedOpenApiFixtureDeclaresAllEmploymentRateOperations() throws Exception {
        String openApi = StreamUtils.copyToString(
                new ClassPathResource("contracts/openapi.yaml").getInputStream(),
                StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/employment-rate-achievements:")
                .contains("operationId: uploadEmploymentRateAchievementsExcel")
                .contains("operationId: createEmploymentRateBulkJob");
    }

    @Test
    void listCreateGetAndUpdateUseTheApprovedEnvelopeAndRequestId() throws Exception {
        EmploymentRateAchievementRow row = row();
        when(service.list(0, 20, r01)).thenReturn(
                new EmploymentRateAchievementListResponse(List.of(row), 0, 20, 1));
        when(service.create(any(), eq(r01), eq("REQ-ERA-CREATE"))).thenReturn(row);
        when(service.get(501L, r01)).thenReturn(row);
        when(service.update(eq(501L), any(), eq(r01), eq("REQ-ERA-UPDATE"))).thenReturn(row);

        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-ERA-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("ERA-001"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-ERA-LIST"));

        String body = """
                {"managementItemCode":"EMPLOYMENT_RATE","achievementDate":"2026-04-10","achievementName":"취업률 실적"}
                """;
        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-ERA-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(501))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-ERA-CREATE"));

        mockMvc.perform(get("/api/business/employment-rate-achievements/501")
                        .requestAttr("currentUser", r01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementStatus").value("DRAFT"));

        mockMvc.perform(put("/api/business/employment-rate-achievements/501")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-ERA-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.requestId").value("REQ-ERA-UPDATE"));
        verify(service).update(eq(501L), any(), eq(r01), eq("REQ-ERA-UPDATE"));
    }

    @Test
    void uploadAndBulkOperationsRequireR07AndReturnTheirContractStatuses() throws Exception {
        when(service.upload(any(), eq(r07), any())).thenReturn(
                new EmploymentRateExcelUploadResponse(false, 0, 1, "오류 행", "ERR-FILE"));
        MockMultipartFile file = new MockMultipartFile("file", "rows.csv", "text/csv", "header\nE9999".getBytes());

        mockMvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(file)
                        .requestAttr("currentUser", r07))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applied").value(false))
                .andExpect(jsonPath("$.data.errorCount").value(1));

        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{"
                                        + "\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\","
                                        + "\"targetCondition\":{}"
                                        + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).createBulkJob(any(), any(), any());
    }

    @Test
    void r07CanCreateAndRetrievePersistedBulkJob() throws Exception {
        EmploymentRateBulkJobResponse bulkJob = new EmploymentRateBulkJobResponse(
                "ERB-001",
                "2026",
                "GENERATE",
                "REQUESTED",
                1,
                0,
                1,
                LocalDateTime.parse("2026-04-10T09:00:00"),
                List.of(new EmploymentRateBulkJobItem(101L, null, false, "처리 대기")));
        when(service.createBulkJob(any(), eq(r07), eq("REQ-BULK"))).thenReturn(bulkJob);
        when(service.getBulkJob("ERB-001", r07)).thenReturn(bulkJob);
        String body = """
                {"evaluationYear":"2026","actionType":"GENERATE",
                "targetCondition":{"confirmed":true,"targetUserIds":[101]}}
                """;

        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .requestAttr("currentUser", r07)
                        .header("X-Request-Id", "REQ-BULK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.jobId").value("ERB-001"))
                .andExpect(jsonPath("$.data.items[0].targetUserId").value(101));

        mockMvc.perform(get("/api/business/employment-rate-achievements/bulk-jobs/ERB-001")
                        .requestAttr("currentUser", r07))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobStatus").value("REQUESTED"));
    }

    @Test
    void downloadUsesAttachmentDispositionAndMissingManagementItemIsRejected() throws Exception {
        when(service.list(0, 20, r01)).thenReturn(
                new EmploymentRateAchievementListResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/employment-rate-achievements/download")
                        .requestAttr("currentUser", r01))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));

        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2026-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private EmploymentRateAchievementRow row() {
        return new EmploymentRateAchievementRow(
                501L,
                "ERA-001",
                101L,
                "faculty",
                "2026",
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                "취업률 실적",
                "DRAFT",
                "[]",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}

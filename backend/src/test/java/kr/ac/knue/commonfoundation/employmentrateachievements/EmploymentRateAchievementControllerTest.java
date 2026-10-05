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

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
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

/** MockMvc contract tests for the controller owning every approved employment-rate route. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateAchievementControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateAchievementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L, "excel-operator", "E0107", "엑셀담당자", List.of("R07"), List.of());
    private final CurrentUser unauthorizedUser = new CurrentUser(
            109L, "unauthorized", "E0109", "권한없음", List.of("R09"), List.of());

    @Test
    void approvedOpenApiFixtureDeclaresEveryEmploymentRateOperation() throws Exception {
        String contract = StreamUtils.copyToString(
                new ClassPathResource("contracts/openapi.yaml").getInputStream(),
                StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(contract)
                .contains("operationId: listEmploymentRateAchievements")
                .contains("operationId: createEmploymentRateAchievement")
                .contains("operationId: updateEmploymentRateAchievement")
                .contains("operationId: uploadEmploymentRateAchievementsExcel")
                .contains("operationId: createEmploymentRateBulkJob");
    }

    @Test
    void listAndDetailReturnTheApprovedEnvelopeForAnR01Caller() throws Exception {
        when(service.list(eq(r01), eq(0), eq(20), eq(null), eq(null))).thenReturn(
                new EmploymentRateAchievementSearchResponse(List.of(seedRow()), 0, 20, 1));
        when(service.get(91L, r01)).thenReturn(seedRow());

        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERA-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B83-ERA-001"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-ERA-LIST"));

        mockMvc.perform(get("/api/business/employment-rate-achievements/91")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementName").value("취업률 실적"));
    }

    @Test
    void createUpdateAndDownloadUseTheOwningControllerContract() throws Exception {
        EmploymentRateAchievementSaveResponse saved = new EmploymentRateAchievementSaveResponse(
                seedRow(), false, null);
        when(service.create(any(), eq(r01), eq("REQ-B83-ERA-CREATE"))).thenReturn(saved);
        when(service.update(eq(91L), any(), eq(r01), eq("REQ-B83-ERA-UPDATE"))).thenReturn(saved);
        when(service.download(eq(r01), eq(0), eq(20))).thenReturn(
                "관리번호\nB83-ERA-001".getBytes(StandardCharsets.UTF_8));

        String body = """
                {"managementItemCode":"EMPLOYMENT_RATE","achievementDate":"2026-04-10","achievementName":"취업률 실적"}
                """;
        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERA-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementId").value(91));

        mockMvc.perform(put("/api/business/employment-rate-achievements/91")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERA-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-ERA-UPDATE"));

        mockMvc.perform(get("/api/business/employment-rate-achievements/download")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")))
                .andExpect(header().string(
                        "Content-Type",
                        org.hamcrest.Matchers.startsWith("text/csv")));
    }

    @Test
    void downloadRejectsAnAuthenticatedCallerWithoutEmploymentRateReadRole() throws Exception {
        mockMvc.perform(get("/api/business/employment-rate-achievements/download")
                        .requestAttr("currentUser", unauthorizedUser)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).download(any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void rejectsInvalidR01PayloadAndR01ExcelUpload() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2026-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(service, never()).create(any(), any(), any());

        MockMultipartFile file = new MockMultipartFile(
                "file", "employment-rate.xlsx", "application/vnd.ms-excel", "row".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(file)
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void bulkJobRemainsConflictUntilOq8301IsApprovedAndR07CanReadResult() throws Exception {
        when(service.createBulkJob(any(), eq(r07), eq("REQ-B83-BULK")))
                .thenThrow(new ConflictException("BULK_POLICY_PENDING"));
        when(service.getBulkJob("B83-ERB-001", r07)).thenReturn(new EmploymentRateBulkJobResponse(
                "B83-ERB-001", "2026", "GENERATE", "REQUESTED", 3, 0, 0, 0,
                LocalDateTime.parse("2026-04-10T09:00:00"), null));

        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-BULK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));

        mockMvc.perform(get("/api/business/employment-rate-achievements/bulk-jobs/B83-ERB-001")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchJobId").value("B83-ERB-001"));
    }

    private EmploymentRateAchievementRow seedRow() {
        return new EmploymentRateAchievementRow(
                91L,
                "B83-ERA-001",
                101L,
                "faculty",
                "2026",
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                "취업률 실적",
                "[]",
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

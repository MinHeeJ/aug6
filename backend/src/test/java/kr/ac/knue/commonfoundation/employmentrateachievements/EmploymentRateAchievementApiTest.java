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
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.AuthController;
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

/** MockMvc contract coverage for every approved employment-rate achievement operation. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateAchievementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L, "excel-operator", "E0107", "엑셀담당자", List.of("R07"), List.of());

    @Test
    void approvedOpenApiFixtureDeclaresEveryEmploymentRateOperation() throws Exception {
        String openApi = StreamUtils.copyToString(
                new ClassPathResource("contracts/openapi.yaml").getInputStream(),
                StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/employment-rate-achievements:")
                .contains("operationId: listEmploymentRateAchievements")
                .contains("operationId: createEmploymentRateAchievement")
                .contains("operationId: updateEmploymentRateAchievement")
                .contains("operationId: uploadEmploymentRateAchievementsExcel")
                .contains("operationId: createEmploymentRateBulkJob");
    }

    @Test
    void listsAndRetrievesR01ScopedAchievements() throws Exception {
        EmploymentRateAchievementRow row = row(91L, "DRAFT");
        when(service.list(0, 20, r01)).thenReturn(
                new EmploymentRateAchievementSearchResponse(List.of(row), 0, 20, 1));
        when(service.get(91L, r01)).thenReturn(row);

        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .header("X-Request-Id", "REQ-ER-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].achievementId").value(91))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-ER-LIST"));

        mockMvc.perform(get("/api/business/employment-rate-achievements/91")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("EMPLOYMENT_RATE"));
    }

    @Test
    void createsAndUpdatesWithRequestIdAndRejectsNonR01Write() throws Exception {
        EmploymentRateAchievementSaveResponse saved = new EmploymentRateAchievementSaveResponse(
                row(91L, "DRAFT"), false, null);
        when(service.create(any(), eq(r01), eq("REQ-ER-CREATE"))).thenReturn(saved);
        when(service.update(eq(91L), any(), eq(r01), eq("REQ-ER-UPDATE"))).thenReturn(saved);
        String body = "{\"managementItemCode\":\"EMPLOYMENT_RATE\","
                + "\"achievementDate\":\"2026-04-15\","
                + "\"achievementName\":\"취업률 실적\","
                + "\"attachmentIds\":[\"ATT-1\"]}";

        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .header("X-Request-Id", "REQ-ER-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementName").value("취업률 실적"));

        mockMvc.perform(put("/api/business/employment-rate-achievements/91")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .header("X-Request-Id", "REQ-ER-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.requestId").value("REQ-ER-UPDATE"));

        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r07)
                        .cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), eq(r07), any());
    }

    @Test
    void rejectsMissingManagementItemBeforeServiceMutation() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2026-04-15\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());

        verify(service, never()).create(any(), eq(r01), any());
    }

    @Test
    void downloadsForR07AndValidatesR07Upload() throws Exception {
        when(service.download(0, 20, r07)).thenReturn("achievementId\n91\n".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employment-rate.csv",
                "text/csv",
                ("managementItemCode,achievementDate,achievementName,attachmentIds\n"
                        + "EMPLOYMENT_RATE,2026-04-15,취업률 실적,")
                        .getBytes(StandardCharsets.UTF_8));
        when(service.upload(any(), eq(r07), eq("REQ-ER-UPLOAD"))).thenReturn(
                new EmploymentRateExcelUploadResult("ER-UP-001", "employment-rate.csv", 1, 1, 0, List.of()));

        mockMvc.perform(get("/api/business/employment-rate-achievements/download")
                        .requestAttr("currentUser", r07)
                        .cookie(cookie()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));

        mockMvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(file)
                        .requestAttr("currentUser", r07)
                        .cookie(cookie())
                        .header("X-Request-Id", "REQ-ER-UPLOAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1));
    }

    @Test
    void returnsR07BatchResultAndRejectsR01BatchRequest() throws Exception {
        when(service.getBulkJob("B83-BATCH-001", r07)).thenReturn(new EmploymentRateBulkJobResponse(
                "B83-BATCH-001",
                "2026",
                Map.of("organizationCode", "KNUE-DEPT-COMP"),
                "GENERATE",
                "COMPLETED",
                3,
                3,
                0,
                0,
                "REQ-B83-BATCH-001"));

        mockMvc.perform(get("/api/business/employment-rate-achievements/bulk-jobs/B83-BATCH-001")
                        .requestAttr("currentUser", r07)
                        .cookie(cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobStatus").value("COMPLETED"));

        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\",\"targetCondition\":{}}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private EmploymentRateAchievementRow row(Long achievementId, String status) {
        return new EmploymentRateAchievementRow(
                achievementId,
                101L,
                "faculty",
                "2026",
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-15"),
                "취업률 실적",
                List.of("ATT-1"),
                status,
                "2026-04-15T09:00:00",
                "2026-04-15T09:00:00");
    }

    private Cookie cookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

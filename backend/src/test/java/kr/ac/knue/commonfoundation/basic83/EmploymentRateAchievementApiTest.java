package kr.ac.knue.commonfoundation.basic83;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** MockMvc contract coverage for the controller owning employment-rate routes. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateAchievementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L,
            "operator",
            "E0107",
            "담당자",
            List.of("R07"),
            List.of());

    @Test
    void listReturnsApprovedEnvelope() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(
                new EmploymentRateAchievementSearchResponse(
                        List.of(row()),
                        0,
                        20,
                        1));

        mockMvc.perform(
                        get("/api/business/employment-rate-achievements")
                                .requestAttr("currentUser", r01)
                                .cookie(cookie())
                                .header("X-Request-Id", "ERA-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B83-ERA-001"))
                .andExpect(jsonPath("$.meta.requestId").value("ERA-LIST"));
    }

    @Test
    void createPersistsIndividualAchievement() throws Exception {
        when(service.create(any(), eq(r01), eq("ERA-CREATE"))).thenReturn(
                new EmploymentRateAchievementSaveResult(row(), false, null));

        mockMvc.perform(
                        post("/api/business/employment-rate-achievements")
                                .requestAttr("currentUser", r01)
                                .cookie(cookie())
                                .header("X-Request-Id", "ERA-CREATE")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.achievement.managementItemCode")
                                .value("EMPLOYMENT_RATE"));
        verify(service).create(any(), eq(r01), eq("ERA-CREATE"));
    }

    @Test
    void missingManagementItemDoesNotReachService() throws Exception {
        mockMvc.perform(
                        post("/api/business/employment-rate-achievements")
                                .requestAttr("currentUser", r01)
                                .cookie(cookie())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"achievementDate\":\"2026-04-10\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void r07CanDownloadAnOfficeOpenXmlWorkbook() throws Exception {
        byte[] workbook = new byte[] {'P', 'K', 3, 4};
        when(service.download(any(), eq(r07))).thenReturn(workbook);

        mockMvc.perform(
                        get("/api/business/employment-rate-achievements/download")
                                .requestAttr("currentUser", r07)
                                .cookie(cookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=employment-rate-achievements.xlsx"))
                .andExpect(content().bytes(workbook));
        verify(service).download(any(), eq(r07));
    }

    @Test
    void r07CanReadPersistedBulkJobOutcomes() throws Exception {
        when(service.getBulkJob(eq("JOB-83"), eq(r07))).thenReturn(
                new EmploymentRateBulkJobResponse(
                        "JOB-83",
                        "2026",
                        "GENERATE",
                        "COMPLETED",
                        107L,
                        LocalDateTime.parse("2026-04-10T09:00:00"),
                        LocalDateTime.parse("2026-04-10T09:01:00"),
                        1,
                        1,
                        0,
                        0,
                        List.of(new EmploymentRateBulkJobItemRow(
                                1L,
                                101L,
                                "PROCESSED",
                                "완료",
                                LocalDateTime.parse("2026-04-10T09:01:00")))));

        mockMvc.perform(
                        get("/api/business/employment-rate-achievements/bulk-jobs/JOB-83")
                                .requestAttr("currentUser", r07)
                                .cookie(cookie())
                                .header("X-Request-Id", "ERA-JOB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value("JOB-83"))
                .andExpect(jsonPath("$.data.items[0].processingStatus").value("PROCESSED"))
                .andExpect(jsonPath("$.meta.requestId").value("ERA-JOB"));
    }

    @Test
    void nonR07BulkRequestIsForbidden() throws Exception {
        mockMvc.perform(
                        post("/api/business/employment-rate-achievements/bulk-jobs")
                                .requestAttr("currentUser", r01)
                                .cookie(cookie())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unresolvedBulkPolicyReturnsConflictBeforeJobCreation() throws Exception {
        mockMvc.perform(
                        post("/api/business/employment-rate-achievements/bulk-jobs")
                                .requestAttr("currentUser", r07)
                                .cookie(cookie())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isConflict());
    }

    private EmploymentRateAchievementRow row() {
        return new EmploymentRateAchievementRow(
                81L,
                "B83-ERA-001",
                101L,
                "faculty",
                "2026",
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                "취업률 제고",
                null,
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private String validBody() {
        return "{\"managementItemCode\":\"EMPLOYMENT_RATE\","
                + "\"achievementDate\":\"2026-04-10\","
                + "\"achievementName\":\"취업률 실적\"}";
    }

    private Cookie cookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

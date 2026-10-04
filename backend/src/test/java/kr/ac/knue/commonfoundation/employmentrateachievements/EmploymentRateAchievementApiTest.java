package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** MockMvc contract coverage for the controller that owns the BASIC-83 employment-rate routes. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateAchievementService service;

    private final CurrentUser r01 = new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(107L, "operator", "E0107", "운영자", List.of("R07"), List.of());

    @Test
    void listValidatesPagingAndPreservesTheApiEnvelope() throws Exception {
        when(service.list(0, 20, r01)).thenReturn(new EmploymentRateAchievementViews.SearchResponse(
                List.of(achievement()), 0, 20, 1));

        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-83-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].achievementId").value(83))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-83-LIST"));
        mockMvc.perform(get("/api/business/employment-rate-achievements?pageSize=10")
                        .requestAttr("currentUser", r01))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("pageSize"));
    }

    @Test
    void createRequiresR01AtTheServiceBoundaryAndBulkRequestUsesR07() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-83-CREATE"))).thenReturn(achievement());
        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-83-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EMPLOYMENT_RATE\",\"achievementDate\":\"2026-04-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementNo").value("ER-83"));
        verify(service).create(any(), eq(r01), eq("REQ-83-CREATE"));

        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("actionType"));
    }

    @Test
    void downloadReturnsOpenApiXlsxContentType() throws Exception {
        when(service.download(0, 20, r07)).thenReturn(new EmploymentRateAchievementViews.SearchResponse(
                List.of(achievement()), 0, 20, 1));

        mockMvc.perform(get("/api/business/employment-rate-achievements/download")
                        .requestAttr("currentUser", r07))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    private EmploymentRateAchievementViews.Achievement achievement() {
        return new EmploymentRateAchievementViews.Achievement(
                83L,
                "ER-83",
                101L,
                "faculty",
                "2026",
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-01"),
                "취업률 실적",
                List.of(),
                "DRAFT",
                LocalDateTime.parse("2026-04-01T09:00:00"),
                LocalDateTime.parse("2026-04-01T09:00:00"));
    }
}

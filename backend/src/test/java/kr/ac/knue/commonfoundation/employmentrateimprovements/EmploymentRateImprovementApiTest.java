package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** MockMvc contract tests for the controller that owns the four 취업률 제고 API operations. */
@WebMvcTest(EmploymentRateImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateImprovementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateImprovementService service;

    private final CurrentUser r01 = new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(107L, "operator", "E0107", "담당자", List.of("R07"), List.of());

    @Test
    void listReturnsApprovedEnvelope() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(
                new EmploymentRateImprovementSearchResponse(List.of(row()), 0, 20, 1));
        mockMvc.perform(get("/api/business/employment-rate-improvements").requestAttr("currentUser", r01)
                        .header("X-Request-Id", "ERI-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B83-ERI-001"))
                .andExpect(jsonPath("$.meta.requestId").value("ERI-LIST"));
    }

    @Test
    void createRejectsR07() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"managementItemCode\":\"EMPLOYMENT_RATE_IMPROVEMENT\",\"achievementDate\":\"2026-04-10\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void createRejectsMissingManagementItemCode() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2026-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getAndUpdateReachTheOwningController() throws Exception {
        when(service.get(82L, r01)).thenReturn(row());
        when(service.update(eq(82L), any(), eq(r01), eq("ERI-UPDATE")))
                .thenReturn(new EmploymentRateImprovementSaveResult(row(), false, null));
        mockMvc.perform(get("/api/business/employment-rate-improvements/82").requestAttr("currentUser", r01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(82));
        mockMvc.perform(put("/api/business/employment-rate-improvements/82").requestAttr("currentUser", r01)
                        .header("X-Request-Id", "ERI-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"managementItemCode\":\"EMPLOYMENT_RATE_IMPROVEMENT\",\"achievementDate\":\"2026-04-11\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.requestId").value("ERI-UPDATE"));
    }

    private EmploymentRateImprovementRow row() {
        return new EmploymentRateImprovementRow(
                82L,
                "B83-ERI-001",
                101L,
                "faculty",
                "KNUE-DEPT-COMP",
                "2026",
                "EMPLOYMENT_RATE_IMPROVEMENT",
                LocalDate.parse("2026-04-10"),
                "취업률 제고",
                "DRAFT",
                "[]",
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-10"),
                "2026-03-01~2026-03-07",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}

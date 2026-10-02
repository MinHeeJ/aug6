package kr.ac.knue.commonfoundation.basic83;

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

/** MockMvc contract coverage for the controller that owns BASIC-83 취업률 제고 routes. */
@WebMvcTest(EmploymentRateImprovementAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateImprovementAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateImprovementAchievementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L, "operator", "E0107", "담당자", List.of("R07"), List.of());

    @Test
    void listReturnsTheApprovedEnvelope() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(
                new EmploymentRateImprovementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERI-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B83-ERI-001"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-ERI-LIST"));
    }

    @Test
    void createReturnsThePersistedAchievement() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-B83-ERI-CREATE"))).thenReturn(result());

        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERI-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.managementItemCode").value("EMPLOYMENT_RATE"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-ERI-CREATE"));
        verify(service).create(any(), eq(r01), eq("REQ-B83-ERI-CREATE"));
    }

    @Test
    void getReturnsTheSelectedDetail() throws Exception {
        when(service.get(81L, r01)).thenReturn(row());

        mockMvc.perform(get("/api/business/employment-rate-improvements/81")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(81))
                .andExpect(jsonPath("$.data.specialLectureStartDate").value("2026-04-01"));
    }

    @Test
    void updateUsesThePathAchievementId() throws Exception {
        when(service.update(eq(81L), any(), eq(r01), eq("REQ-B83-ERI-UPDATE"))).thenReturn(result());

        mockMvc.perform(put("/api/business/employment-rate-improvements/81")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERI-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementId").value(81));
        verify(service).update(eq(81L), any(), eq(r01), eq("REQ-B83-ERI-UPDATE"));
    }

    @Test
    void missingManagementItemReturnsValidationErrorWithoutServiceCall() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2026-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void r07CannotCreateEmploymentRateImprovement() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any(), any());
    }

    private EmploymentRateImprovementSaveResult result() {
        return new EmploymentRateImprovementSaveResult(row(), false, null);
    }

    private EmploymentRateImprovementRow row() {
        return new EmploymentRateImprovementRow(
                81L,
                "B83-ERI-001",
                101L,
                "faculty",
                "2026",
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-03"),
                "2026-04-15~2026-04-16",
                null,
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private String validBody() {
        return """
                {"managementItemCode":"EMPLOYMENT_RATE","achievementDate":"2026-04-10",
                "specialLectureStartDate":"2026-04-01","specialLectureEndDate":"2026-04-03"}
                """;
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

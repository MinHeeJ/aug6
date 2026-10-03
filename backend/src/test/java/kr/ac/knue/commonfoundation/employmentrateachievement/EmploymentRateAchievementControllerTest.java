package kr.ac.knue.commonfoundation.employmentrateachievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;

/** Contract tests for the controller that owns employment-rate achievement read URLs. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateAchievementControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateAchievementService service;

    private final CurrentUser r01 = user(101L, "R01");
    private final CurrentUser r02 = user(102L, "R02");
    private final CurrentUser r04 = user(104L, "R04");
    private final CurrentUser r07 = user(107L, "R07");

    @Test
    void r01ListPassesTheOwnerToTheScopedService() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(response());

        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].targetUserId").value(101))
                .andExpect(jsonPath("$.data.page").value(0));

        verify(service).list(new EmploymentRateAchievementSearchCriteria(0, 20), r01);
    }

    @Test
    void r02AndR04CanReachTheReadControllerForTheirServiceScopedVisibility() throws Exception {
        when(service.list(any(), eq(r02))).thenReturn(response());
        when(service.list(any(), eq(r04))).thenReturn(response());

        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r02))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r04))
                .andExpect(status().isOk());

        verify(service).list(new EmploymentRateAchievementSearchCriteria(0, 20), r02);
        verify(service).list(new EmploymentRateAchievementSearchCriteria(0, 20), r04);
    }

    @Test
    void unauthorizedRoleIsRejectedBeforeTheService() throws Exception {
        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r07))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).list(any(), any());
    }

    @Test
    void negativePageUsesTheStandardValidationEnvelope() throws Exception {
        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("page"));

        verify(service, never()).list(any(), any());
    }

    @Test
    void unsupportedPageSizeUsesTheStandardValidationEnvelope() throws Exception {
        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("pageSize"));

        verify(service, never()).list(any(), any());
    }

    private CurrentUser user(Long userId, String role) {
        return new CurrentUser(userId, "user" + userId, "E" + userId, "사용자", List.of(role), List.of());
    }

    private EmploymentRateAchievementSearchResponse response() {
        EmploymentRateAchievementRow row = new EmploymentRateAchievementRow(
                1L,
                101L,
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                "취업률 실적",
                List.of(),
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
        return new EmploymentRateAchievementSearchResponse(List.of(row), 0, 20, 1);
    }
}

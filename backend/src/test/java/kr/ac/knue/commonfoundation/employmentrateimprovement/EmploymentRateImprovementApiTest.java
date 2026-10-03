package kr.ac.knue.commonfoundation.employmentrateimprovement;

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
import java.nio.charset.StandardCharsets;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

/** MockMvc contract tests for the controller that owns the 취업률 제고 routes. */
@WebMvcTest(EmploymentRateImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateImprovementApiTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private EmploymentRateImprovementService service;
    private final CurrentUser r01 = new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(107L, "operator", "E0107", "담당자", List.of("R07"), List.of());

    @Test
    void contractFixtureIsAvailableToBackendTests() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(openApi).contains("openapi:");
    }

    @Test
    void listReturnsApprovedEnvelope() throws Exception {
        when(service.list(0, 20, r01)).thenReturn(
                new EmploymentRateImprovementSearchResponse(List.of(row()), 0, 20, 1));
        mockMvc.perform(get("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .header("X-Request-Id", "REQ-B83-ERI-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode")
                        .value("EMPLOYMENT_RATE_IMPROVEMENT"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-ERI-LIST"));
    }

    @Test
    void getReturnsDetailForAuthorizedUser() throws Exception {
        when(service.get(71L, r01)).thenReturn(row());
        mockMvc.perform(get("/api/business/employment-rate-improvements/71")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(71));
    }

    @Test
    void createReturnsPersistedValue() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-B83-ERI-CREATE")))
                .thenReturn(new EmploymentRateImprovementSaveResult(row()));
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .header("X-Request-Id", "REQ-B83-ERI-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"managementItemCode\":\"EMPLOYMENT_RATE_IMPROVEMENT\",\"achievementDate\":\"2026-04-10\",\"specialLectureStartDate\":\"2026-04-01\",\"specialLectureEndDate\":\"2026-04-10\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementId").value(71));
        verify(service).create(any(), eq(r01), eq("REQ-B83-ERI-CREATE"));
    }

    @Test
    void updateRejectsMissingManagementItemCode() throws Exception {
        mockMvc.perform(put("/api/business/employment-rate-improvements/71")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2026-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        verify(service, never()).update(any(), any(), any(), any());
    }

    @Test
    void r07CannotCreate() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r07)
                        .cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"managementItemCode\":\"EMPLOYMENT_RATE_IMPROVEMENT\",\"achievementDate\":\"2026-04-10\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any(), any());
    }

    private EmploymentRateImprovementRow row() {
        return new EmploymentRateImprovementRow(
                71L,
                101L,
                "faculty",
                "2026",
                "EMPLOYMENT_RATE_IMPROVEMENT",
                LocalDate.parse("2026-04-10"),
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-10"),
                "문항 출제",
                "[]",
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private Cookie cookie() { return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"); }
}

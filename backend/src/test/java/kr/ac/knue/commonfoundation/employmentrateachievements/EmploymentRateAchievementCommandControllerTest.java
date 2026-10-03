package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.employmentrateachievement.EmploymentRateAchievementRequest;
import kr.ac.knue.commonfoundation.employmentrateachievement.EmploymentRateAchievementRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Contract tests for the real controller that owns employment-rate write URLs. */
@WebMvcTest(EmploymentRateAchievementCommandController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateAchievementCommandControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateAchievementCommandService service;

    private final CurrentUser r01 = user(101L, "R01");
    private final CurrentUser r07 = user(107L, "R07");

    @Test
    void createAsR01ForwardsTheValidatedRequestAndRequestId() throws Exception {
        EmploymentRateAchievementRequest request = request();
        when(service.create(eq(request), eq(r01), eq("request-83"))).thenReturn(row());

        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EMPLOYMENT_RATE","achievementDate":"2026-04-10",
                                 "achievementName":"취업률 실적","attachmentIds":["ATT-1"]}
                                """)
                        .header("X-Request-Id", "request-83")
                        .requestAttr("currentUser", r01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementId").value(11))
                .andExpect(jsonPath("$.meta.requestId").value("request-83"));

        verify(service).create(request, r01, "request-83");
    }

    @Test
    void createRejectsARoleWithoutR01BeforeTheCommandService() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"managementItemCode\":\"EMPLOYMENT_RATE\","
                                + "\"achievementDate\":\"2026-04-10\"}")
                        .requestAttr("currentUser", r07))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void createReturnsTheStandardValidationEnvelopeForMissingManagementItemCode() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"achievementDate\":\"2026-04-10\"}")
                        .requestAttr("currentUser", r01))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode"));

        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void updateAsR01UsesThePathIdentifierAndGeneratedRequestId() throws Exception {
        EmploymentRateAchievementRequest request = request();
        when(service.update(eq(11L), eq(request), eq(r01), any())).thenReturn(row());

        mockMvc.perform(put("/api/business/employment-rate-achievements/{achievementId}", 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EMPLOYMENT_RATE","achievementDate":"2026-04-10",
                                 "achievementName":"취업률 실적","attachmentIds":["ATT-1"]}
                                """)
                        .requestAttr("currentUser", r01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(11))
                .andExpect(jsonPath("$.meta.requestId").isNotEmpty());

        verify(service).update(eq(11L), eq(request), eq(r01), any());
    }

    @Test
    void materializedOpenApiFixtureIsAvailableOnTheBackendTestClasspath() {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");

        org.assertj.core.api.Assertions.assertThat(contract.exists()).isTrue();
    }

    private EmploymentRateAchievementRequest request() {
        return new EmploymentRateAchievementRequest(
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                "취업률 실적",
                List.of("ATT-1"));
    }

    private EmploymentRateAchievementRow row() {
        return new EmploymentRateAchievementRow(
                11L,
                r01.userId(),
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                "취업률 실적",
                List.of("ATT-1"),
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private CurrentUser user(Long userId, String role) {
        return new CurrentUser(userId, "user" + userId, "E" + userId, "사용자", List.of(role), List.of());
    }
}

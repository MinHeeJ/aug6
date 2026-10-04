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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

/** MockMvc contract coverage for the controller owning employment-rate-improvement routes. */
@WebMvcTest(EmploymentRateImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateImprovementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateImprovementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L, "excel", "E0107", "실적담당", List.of("R07"), List.of());

    @Test
    void approvedOpenApiFixtureDeclaresEmploymentRateImprovementOperations() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/employment-rate-improvements:")
                .contains("operationId: createEmploymentRateImprovement")
                .contains("operationId: updateEmploymentRateImprovement");
    }

    @Test
    void listReturnsApprovedEnvelopeAndRequestId() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(
                new EmploymentRateImprovementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-B83-ERI-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B83-ERI-001"))
                .andExpect(jsonPath("$.data.achievements[0].specialLectureStartDate").value("2026-04-01"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-ERI-LIST"));
    }

    @Test
    void detailReturnsPersistedRow() throws Exception {
        when(service.get(83L, r01)).thenReturn(row());

        mockMvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 83L)
                        .requestAttr("currentUser", r01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(83))
                .andExpect(jsonPath("$.data.mockExamQuestionPeriod").value("2026-1차"));
    }

    @Test
    void createReturnsSavedValue() throws Exception {
        when(service.create(any(), eq(r01))).thenReturn(
                new EmploymentRateImprovementSaveResponse(row(), false, null));

        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT",
                                "achievementDate":"2026-04-10",
                                "specialLectureStartDate":"2026-04-01",
                                "specialLectureEndDate":"2026-04-10"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.managementItemCode")
                        .value("EMPLOYMENT_RATE_IMPROVEMENT"));
        verify(service).create(any(), eq(r01));
    }

    @Test
    void updateUsesPathIdentifierOnly() throws Exception {
        when(service.update(eq(83L), any(), eq(r01))).thenReturn(
                new EmploymentRateImprovementSaveResponse(row(), true, "기간 경고"));

        mockMvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 83L)
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT","achievementDate":"2026-04-11"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        verify(service).update(eq(83L), any(), eq(r01));
    }

    @Test
    void createRejectsMissingManagementItemCode() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"achievementDate\":\"2026-04-10\"" + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        verify(service, never()).create(any(), any());
    }

    @Test
    void r07CannotCreateEmploymentRateImprovement() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT","achievementDate":"2026-04-10"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any());
    }

    private EmploymentRateImprovementRow row() {
        return new EmploymentRateImprovementRow(
                83L,
                "B83-ERI-001",
                101L,
                "faculty",
                "2026",
                "EMPLOYMENT_RATE_IMPROVEMENT",
                LocalDate.parse("2026-04-10"),
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-10"),
                "2026-1차",
                "[]",
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}

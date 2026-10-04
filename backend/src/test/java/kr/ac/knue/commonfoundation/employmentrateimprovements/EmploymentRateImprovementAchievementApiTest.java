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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

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
            101L,
            "professor1",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L,
            "excel-operator",
            "E0107",
            "엑셀담당자",
            List.of("R07"),
            List.of());

    @Test
    void approvedOpenApiFixtureDeclaresEmploymentRateImprovementOperations() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/employment-rate-improvements:")
                .contains("operationId: listEmploymentRateImprovements")
                .contains("operationId: createEmploymentRateImprovement")
                .contains("operationId: getEmploymentRateImprovement")
                .contains("operationId: updateEmploymentRateImprovement");
    }

    @Test
    void listAndDetailReturnTheApprovedEnvelopeForAnR01Principal() throws Exception {
        when(service.list(0, 20, r01)).thenReturn(new EmploymentRateImprovementSearchResponse(
                List.of(row()),
                0,
                20,
                1));
        when(service.get(501L, r01)).thenReturn(row());

        mockMvc.perform(get("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERI-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].achievementId").value(501))
                .andExpect(jsonPath("$.data.achievements[0].specialLectureStartDate").value("2026-04-01"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-ERI-LIST"));
        mockMvc.perform(get("/api/business/employment-rate-improvements/501")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mockExamQuestionPeriod").value("2026-03-15~2026-03-20"));
    }

    @Test
    void createReturnsPersistedFieldsAndRequestId() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-B83-ERI-CREATE"))).thenReturn(
                new EmploymentRateImprovementSaveResponse(
                        row(),
                        true,
                        "업적발생일이 평가대상 기간 밖입니다. 경고를 확인한 후 저장할 수 있습니다."));

        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERI-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT",
                                  "achievementDate":"2026-04-10",
                                  "specialLectureStartDate":"2026-04-01",
                                  "specialLectureEndDate":"2026-04-10",
                                  "mockExamQuestionPeriod":"2026-03-15~2026-03-20",
                                  "attachmentIds":["B83-ATTACHMENT-001"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.managementItemCode")
                        .value("EMPLOYMENT_RATE_IMPROVEMENT"))
                .andExpect(jsonPath("$.data.achievement.attachmentIds[0]").value("B83-ATTACHMENT-001"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-ERI-CREATE"));
        verify(service).create(any(), eq(r01), eq("REQ-B83-ERI-CREATE"));
    }

    @Test
    void missingManagementItemCodeAndR07WriteAreRejected() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"achievementDate\":\"2026-04-10\"" + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT","achievementDate":"2026-04-10"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), eq(r07), any());
    }

    @Test
    void updateConfirmedAchievementReturns409WithoutReturningTheOriginalData() throws Exception {
        when(service.update(eq(501L), any(), eq(r01), eq("REQ-B83-ERI-CONFIRMED")))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다."));

        mockMvc.perform(put("/api/business/employment-rate-improvements/501")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERI-CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT","achievementDate":"2026-04-10"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    private EmploymentRateImprovementAchievementResponse row() {
        return new EmploymentRateImprovementAchievementResponse(
                501L,
                "professor1",
                "KNUE-DEPT-COMP",
                "2026",
                "EMPLOYMENT_RATE_IMPROVEMENT",
                LocalDate.parse("2026-04-10"),
                "DRAFT",
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-10"),
                "2026-03-15~2026-03-20",
                List.of("B83-ATTACHMENT-001"),
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

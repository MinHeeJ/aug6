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
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
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

/** MockMvc contract coverage for the controller that owns 취업률 제고 routes. */
@WebMvcTest(EmploymentRateImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EmploymentRateImprovementService.class})
class EmploymentRateImprovementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateImprovementMapper mapper;

    @MockBean
    private EducationAchievementGuardService guardService;

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
            "운영자",
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
                .contains("operationId: updateEmploymentRateImprovement");
    }

    @Test
    void listExecutesTheControllerServiceAndMapperPathForAuthorizedUser() throws Exception {
        when(mapper.list(any(), eq(r01.userId()), eq(r01.roles()))).thenReturn(List.of(row()));
        when(mapper.count(any(), eq(r01.userId()), eq(r01.roles()))).thenReturn(1L);

        mockMvc.perform(get("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERI-LIST")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B83-ERI-001"))
                .andExpect(jsonPath("$.data.achievements[0].hasAttachments").value(false))
                .andExpect(jsonPath("$.data.achievements[0].attachmentRefs").doesNotExist())
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-ERI-LIST"));
        verify(mapper).list(any(), eq(r01.userId()), eq(r01.roles()));
    }

    @Test
    void getReturnsSelectedDetailForAuthorizedUser() throws Exception {
        when(mapper.findById(81L)).thenReturn(row());
        when(mapper.countAccessible(81L, r01.userId(), r01.roles())).thenReturn(1L);

        mockMvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 81L)
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(81))
                .andExpect(jsonPath("$.data.mockExamQuestionPeriod").value("2026-1 모의평가 출제"));
    }

    @Test
    void createPersistsThroughMapperForAuthorizedUser() throws Exception {
        when(guardService.validateMutation(eq(r01), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.findByManagementNo(any())).thenReturn(row());

        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERI-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"EMPLOYMENT_IMPROVEMENT",
                                  "achievementDate":"2026-04-10",
                                  "specialLectureStartDate":"2026-04-01",
                                  "specialLectureEndDate":"2026-04-10",
                                  "mockExamQuestionPeriod":"2026-1 모의평가 출제",
                                  "attachmentIds":["attachment-1"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.specialLectureStartDate").value("2026-04-01"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-ERI-CREATE"));
        verify(mapper).insert(
                any(),
                eq(r01.userId()),
                eq("2026"),
                any(),
                any(),
                eq(r01.userId()));
    }

    @Test
    void updateReturnsPersistedValueForOwnedRow() throws Exception {
        when(mapper.findById(81L)).thenReturn(row());
        when(guardService.validateMutation(eq(r01), any())).thenReturn(
                OccurredDateValidation.outsideEvaluationPeriod());

        mockMvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81L)
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-ERI-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EMPLOYMENT_IMPROVEMENT","achievementDate":"2026-04-10"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementDateWarning").value(true))
                .andExpect(jsonPath("$.data.warningMessage").value(
                        "업적발생일이 평가대상 기간 밖입니다. 경고를 확인한 후 저장할 수 있습니다."));
    }

    @Test
    void createRejectsMissingManagementItemCodeBeforeServiceMutation() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"achievementDate\":\"2026-04-10\"" + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        verify(mapper, never()).insert(any(), any(), any(), any(), any(), any());
    }

    @Test
    void r07CannotCreateEmploymentRateImprovement() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EMPLOYMENT_IMPROVEMENT","achievementDate":"2026-04-10"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(mapper, never()).insert(any(), any(), any(), any(), any(), any());
    }

    private EmploymentRateImprovementRow row() {
        return new EmploymentRateImprovementRow(
                81L,
                "B83-ERI-001",
                101L,
                "faculty",
                "2026",
                "EMPLOYMENT_IMPROVEMENT",
                LocalDate.parse("2026-04-10"),
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-10"),
                "2026-1 모의평가 출제",
                "DRAFT",
                false,
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

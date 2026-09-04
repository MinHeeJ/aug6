package kr.ac.knue.commonfoundation.basic50;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(Basic50Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic50BusinessApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean Basic50Service service;

    private final CurrentUser r01 = new CurrentUser(1L, "teacher", "E0001", "교원", List.of("R01"), List.of());
    private final CurrentUser r04 = new CurrentUser(4L, "business", "E0004", "담당자", List.of("R04"), List.of());
    private final CurrentUser r09 = new CurrentUser(9L, "admin", "E0009", "관리자", List.of("R09"), List.of());

    @Test
    void listCollegeEvaluationUnitAuthoritiesReturnsScopeAndRequestId() throws Exception {
        when(service.listAuthorities(eq(r04), any())).thenReturn(new AuthoritySearchResponse(List.of(authority()), 0, 20, 1));
        mockMvc.perform(get("/api/business/college-evaluation-unit-authorities")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B50-AUTH-LIST")
                        .param("evaluationYear", "2027").param("organizationCode", "COL-EDU").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorities[0].organizationCode").value("COL-EDU"))
                .andExpect(jsonPath("$.data.authorities[0].modifyAllowedYn").value("N"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B50-AUTH-LIST"));
    }

    @Test
    void saveAppealBusinessSettingReturnsPersistedRow() throws Exception {
        when(service.saveAppealSetting(any(), eq(r09), eq("REQ-B50-APPEAL-SAVE"))).thenReturn(setting());
        mockMvc.perform(post("/api/business/appeal-business-settings/save")
                        .requestAttr("currentUser", r09).cookie(sessionCookie()).header("X-Request-Id", "REQ-B50-APPEAL-SAVE")
                        .contentType(MediaType.APPLICATION_JSON).content(settingJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingId").value(701))
                .andExpect(jsonPath("$.data.activeYn").value("Y"));
    }

    @Test
    void saveResultViewBusinessSettingRequiresFieldErrors() throws Exception {
        mockMvc.perform(post("/api/business/result-view-business-settings/save")
                        .requestAttr("currentUser", r09).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("evaluationYear")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("organizationCode")));
        verify(service, never()).saveResultSetting(any(), any(), any());
    }

    @Test
    void getPersonalAchievementScoresBlocksR01OtherUserInServiceContract() {
        Basic50Mapper mapper = org.mockito.Mockito.mock(Basic50Mapper.class);
        Basic50Service actual = new Basic50Service(mapper);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> actual.personalScores(r01, 2L, "2027", null, "REQ-B50-SCORE-FORBID"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ForbiddenException.class);
        verify(mapper, never()).listPersonalScoreItems(any(), any(), any());
    }

    @Test
    void getPersonalAchievementScoresReturnsSummaryItemsAndEvidenceLink() throws Exception {
        when(service.personalScores(eq(r01), eq(1L), eq("2027"), eq("RESEARCH"), eq("REQ-B50-SCORE"))).thenReturn(scoreResponse());
        mockMvc.perform(get("/api/business/personal-achievement-scores")
                        .requestAttr("currentUser", r01).cookie(sessionCookie()).header("X-Request-Id", "REQ-B50-SCORE")
                        .param("teacherUserId", "1").param("evaluationYear", "2027").param("areaCode", "RESEARCH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalScore").value(40.0))
                .andExpect(jsonPath("$.data.items[0].ruleName").value("논문실적 세부규정"))
                .andExpect(jsonPath("$.data.items[0].evidenceUrl").value("/admin/score-calculation-histories?scoreId=2"));
    }

    @Test
    void saveResearchCriterionRejectsUnauthorizedRole() throws Exception {
        when(service.saveCriterion(any(), eq(r01), any())).thenThrow(new kr.ac.knue.commonfoundation.common.api.ForbiddenException());
        mockMvc.perform(post("/api/business/research-classification-criteria/save")
                        .requestAttr("currentUser", r01).cookie(sessionCookie()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"areaCode":"RESEARCH","areaName":"연구","managementCriterionCode":"JOURNAL","managementCriterionName":"학술지","activeYn":"Y","changeReason":"저장"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void confirmResearchAchievementRejectsAlreadyConfirmedWithoutMutation() {
        Basic50Mapper mapper = org.mockito.Mockito.mock(Basic50Mapper.class);
        Basic50Service actual = new Basic50Service(mapper);
        ResearchAchievementRow confirmed = achievement("CONFIRMED");
        when(mapper.findResearchAchievementById(703L)).thenReturn(confirmed);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> actual.confirmAchievement(703L, new ResearchAchievementConfirmationRequest("JOURNAL", "확인"), r04, "REQ-B50-CONFIRM-LOCK"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_ACHIEVEMENT_LOCKED");
        verify(mapper, never()).confirmResearchAchievement(any(), any(), any());
    }

    @Test
    void listUnconfirmedResearchAchievementsReturnsOnlySearchContractShape() throws Exception {
        when(service.listUnconfirmedAchievements(eq(r04), any(), eq("REQ-B50-UNCONFIRMED"))).thenReturn(new ResearchAchievementSearchResponse(List.of(achievement("UNCONFIRMED")), 0, 20, 1));
        mockMvc.perform(get("/api/business/unconfirmed-research-achievements")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B50-UNCONFIRMED")
                        .param("evaluationYear", "2027").param("confirmationStatus", "UNCONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].confirmationStatus").value("UNCONFIRMED"))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }


    @Test
    void listAppealBusinessSettingsReturnsBodyContractAndRequestId() throws Exception {
        when(service.listAppealSettings(eq(r04), any())).thenReturn(new BusinessSettingSearchResponse(List.of(setting()), 0, 20, 1));
        mockMvc.perform(get("/api/business/appeal-business-settings")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B50-APPEAL-LIST")
                        .param("evaluationYear", "2027").param("organizationCode", "COL-EDU").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settings[0].organizationCode").value("COL-EDU"))
                .andExpect(jsonPath("$.data.settings[0].activeYn").value("Y"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B50-APPEAL-LIST"));
    }

    @Test
    void listResultViewBusinessSettingsReturnsBodyContractAndRequestId() throws Exception {
        when(service.listResultSettings(eq(r04), any())).thenReturn(new BusinessSettingSearchResponse(List.of(setting()), 0, 20, 1));
        mockMvc.perform(get("/api/business/result-view-business-settings")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B50-RESULT-LIST")
                        .param("evaluationYear", "2027").param("activeYn", "Y").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settings[0].targetScope").value("COLLEGE"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B50-RESULT-LIST"));
    }

    @Test
    void listResearchClassificationCriteriaReturnsCriteriaContractShape() throws Exception {
        when(service.listCriteria(eq(r04), any())).thenReturn(new ResearchCriterionSearchResponse(List.of(criterion()), 0, 20, 1));
        mockMvc.perform(get("/api/business/research-classification-criteria")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B50-CRITERIA-LIST")
                        .param("areaCode", "RESEARCH").param("managementCriterionCode", "JOURNAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria[0].areaCode").value("RESEARCH"))
                .andExpect(jsonPath("$.data.criteria[0].managementCriterionCode").value("JOURNAL"))
                .andExpect(jsonPath("$.data.criteria[0].classifiedAchievementCount").value(3));
    }

    @Test
    void saveCollegeEvaluationUnitAuthorityChecksSideEffectContractResponse() throws Exception {
        when(service.saveAuthority(any(), eq(r09), eq("REQ-B50-AUTH-SAVE"))).thenReturn(authority());
        mockMvc.perform(post("/api/business/college-evaluation-unit-authorities/save")
                        .requestAttr("currentUser", r09).cookie(sessionCookie()).header("X-Request-Id", "REQ-B50-AUTH-SAVE")
                        .contentType(MediaType.APPLICATION_JSON).content(authorityJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorityId").value(801))
                .andExpect(jsonPath("$.data.inputAllowedYn").value("Y"))
                .andExpect(jsonPath("$.data.modifyAllowedYn").value("N"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B50-AUTH-SAVE"));
    }

    @Test
    void saveCollegeEvaluationUnitAuthorityRejectsMissingOrganizationCode() throws Exception {
        mockMvc.perform(post("/api/business/college-evaluation-unit-authorities/save")
                        .requestAttr("currentUser", r09).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"evaluationYear\":\"2027\",\"evaluationUnitCode\":\"UNIT-EDU\",\"managerUserId\":4,\"inputAllowedYn\":\"Y\",\"outputAllowedYn\":\"Y\",\"modifyAllowedYn\":\"N\",\"effectiveStartDate\":\"2027-03-01\",\"effectiveEndDate\":\"2028-02-29\",\"activeYn\":\"Y\",\"changeReason\":\"지정\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("organizationCode")));
        verify(service, never()).saveAuthority(any(), any(), any());
    }

    @Test
    void confirmUnconfirmedResearchAchievementReturnsConfirmedStateAndAuditMetadata() throws Exception {
        when(service.confirmAchievement(eq(703L), any(), eq(r04), eq("REQ-B50-CONFIRM"))).thenReturn(achievement("CONFIRMED"));
        mockMvc.perform(post("/api/business/unconfirmed-research-achievements/703/confirmation")
                        .requestAttr("currentUser", r04).cookie(sessionCookie()).header("X-Request-Id", "REQ-B50-CONFIRM")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"managementCriterionCode\":\"JOURNAL\",\"changeReason\":\"확인\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(703))
                .andExpect(jsonPath("$.data.confirmationStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.managementCriterionCode").value("JOURNAL"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B50-CONFIRM"));
    }

    @Test
    void confirmUnconfirmedResearchAchievementRejectsMissingClassificationCode() throws Exception {
        mockMvc.perform(post("/api/business/unconfirmed-research-achievements/703/confirmation")
                        .requestAttr("currentUser", r04).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"changeReason\":\"확인\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("managementCriterionCode")));
        verify(service, never()).confirmAchievement(any(), any(), any(), any());
    }

    private BusinessSettingRow setting() { return new BusinessSettingRow(701L, "2027", "COL-EDU", "UNIT-EDU", LocalDate.parse("2027-06-01"), LocalDate.parse("2027-06-15"), 4L, "USER-4", "COLLEGE", "Y", "저장", 9L, 9L, LocalDateTime.parse("2026-09-04T09:00:00"), LocalDateTime.parse("2026-09-04T09:00:00")); }
    private AuthorityRow authority() { return new AuthorityRow(801L, "2027", "COL-EDU", "UNIT-EDU", 4L, "USER-4", "Y", "Y", "N", null, null, LocalDate.parse("2027-03-01"), LocalDate.parse("2028-02-29"), "Y", "지정", 9L, 9L, LocalDateTime.parse("2026-09-04T09:00:00"), LocalDateTime.parse("2026-09-04T09:00:00")); }
    private ResearchCriterionRow criterion() { return new ResearchCriterionRow(901L, "RESEARCH", "연구", "JOURNAL", "학술지", null, "Y", "저장", 3L, 9L, 9L, LocalDateTime.parse("2026-09-04T09:00:00"), LocalDateTime.parse("2026-09-04T09:00:00")); }
    private ResearchAchievementRow achievement(String status) { return new ResearchAchievementRow(703L, "2027", "COL-EDU", 1L, "교원1", "AI 교육 연구", "RESEARCH", status.equals("CONFIRMED") ? "JOURNAL" : null, status.equals("CONFIRMED") ? "JOURNAL" : null, status, LocalDate.parse("2027-04-01"), "KORUS", null, null, LocalDateTime.parse("2026-09-04T09:00:00")); }
    private PersonalAchievementScoreResponse scoreResponse() { return new PersonalAchievementScoreResponse(1L, "교원1", "2027", new BigDecimal("40.00"), List.of(new PersonalScoreSummary("RESEARCH", "연구", new BigDecimal("40.00"))), List.of(new PersonalScoreItem(2L, "RESEARCH", "연구", "RES-JOURNAL", "논문실적", new BigDecimal("40.00"), "KCI 논문 2편 × 20", "RULE-RES-01", "논문실적 세부규정", "/admin/score-calculation-histories?scoreId=2"))); }
    private String settingJson() { return """
            {"evaluationYear":"2027","organizationCode":"COL-EDU","evaluationUnitCode":"UNIT-EDU","effectiveStartDate":"2027-06-01","effectiveEndDate":"2027-06-15","managerUserId":4,"targetScope":"COLLEGE","activeYn":"Y","changeReason":"저장"}
            """; }
    private String authorityJson() { return """
            {"evaluationYear":"2027","organizationCode":"COL-EDU","evaluationUnitCode":"UNIT-EDU","managerUserId":4,"inputAllowedYn":"Y","outputAllowedYn":"Y","modifyAllowedYn":"N","effectiveStartDate":"2027-03-01","effectiveEndDate":"2028-02-29","activeYn":"Y","changeReason":"지정"}
            """; }
    private Cookie sessionCookie() { return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"); }
}

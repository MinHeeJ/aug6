package kr.ac.knue.commonfoundation.basic70;

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
import kr.ac.knue.commonfoundation.basic60.Basic60Controller;
import kr.ac.knue.commonfoundation.basic60.Basic60Mapper;
import kr.ac.knue.commonfoundation.basic60.Basic60Service;
import kr.ac.knue.commonfoundation.basic60.OperationalSettingResponses;
import kr.ac.knue.commonfoundation.basic60.OperationalSettingRow;
import kr.ac.knue.commonfoundation.basic60.SaveManagementItemEvaluationScoreSettingRequest;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Defines the Phase 4 management-item evaluation-score HTTP and audit contract.
 */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ManagementItemEvaluationScoreSettingsApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean Basic60Service service;

    private final CurrentUser systemAdmin = new CurrentUser(
            1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void r09CanListTheActiveB60Seed003ScoreSettingAndSaveIt() throws Exception {
        OperationalSettingRow scoreSetting = scoreRow("10.00", "20.00", "Y");
        when(service.listScoreSettings(any())).thenReturn(
                new OperationalSettingResponses.ManagementItemEvaluationScoreSettingSearchResponse(
                        List.of(scoreSetting), 0, 20, 1));
        when(service.saveScoreSetting(any(), eq(1L), eq("REQ-B70-SCORE-SAVE"))).thenReturn(scoreSetting);

        mockMvc.perform(get("/api/admin/management-item-evaluation-score-settings")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B70-SCORE-LIST")
                        .param("ruleVersionId", "10")
                        .param("targetScope", "COLLEGE_EDU")
                        .param("areaCode", "EDUCATION")
                        .param("itemCode", "LECTURE")
                        .param("evaluationYear", "2026")
                        .param("elementCode", "COURSE_GROUP")
                        .param("managementItemCode", "ATTENDANCE")
                        .param("organizationCode", "COL-EDU")
                        .param("activeYn", "Y")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings.length()").value(1))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].organizationCode").value("COL-EDU"))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].evaluationScore").value(10.0))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].maxScore").value(20.0))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].activeYn").value("Y"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B70-SCORE-LIST"));

        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B70-SCORE-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scorePayload(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.evaluationScore").value(10.0))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B70-SCORE-SAVE"));
        verify(service).saveScoreSetting(any(), eq(1L), eq("REQ-B70-SCORE-SAVE"));
    }

    /**
     * Red contract for REQ-1838/REQ-1844: a rejected page-size selection is still a
     * traceable request, so clients can correlate the validation result with their audit trail.
     */
    @Test
    void scoreListRejectsUnsupportedPageSizeWhilePreservingTheCallerRequestId() throws Exception {
        mockMvc.perform(get("/api/admin/management-item-evaluation-score-settings")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B70-SCORE-PAGE-SIZE")
                        .param("pageSize", "25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'pageSize')]").isNotEmpty())
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B70-SCORE-PAGE-SIZE"));
        verify(service, never()).listScoreSettings(any());
    }

    @Test
    void r01CannotReadOrSaveManagementItemEvaluationScoreSettings() throws Exception {
        mockMvc.perform(get("/api/admin/management-item-evaluation-score-settings")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scorePayload(10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).saveScoreSetting(any(), any(), any());
    }

    @Test
    void saveWithoutRuleVersionReturnsTheRuleVersionFieldError() throws Exception {
        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scorePayload(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'ruleVersionId')]").isNotEmpty());
        verify(service, never()).saveScoreSetting(any(), any(), any());
    }

    @Test
    void confirmedRuleVersionSaveReturnsTheConfirmedRuleLock() throws Exception {
        when(service.saveScoreSetting(any(), eq(1L), eq("REQ-B70-SCORE-CONFIRMED")))
                .thenThrow(new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B70-SCORE-CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scorePayload(11L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_RULE_LOCKED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B70-SCORE-CONFIRMED"));
    }

    @Test
    void serviceWritesScoreSettingAuditHistoryWithTheRequestId() {
        Basic60Mapper mapper = org.mockito.Mockito.mock(Basic60Mapper.class);
        Basic60Service basic60Service = new Basic60Service(mapper);
        SaveManagementItemEvaluationScoreSettingRequest request = new SaveManagementItemEvaluationScoreSettingRequest(
                10L, "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026", "COURSE_GROUP", "ATTENDANCE",
                "COL-EDU", "사범대학", new BigDecimal("12.50"), new BigDecimal("20.00"), 1, "Y",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "점수 조정");
        OperationalSettingRow before = scoreRow("10.00", "20.00", "Y");
        OperationalSettingRow after = scoreRow("12.50", "20.00", "Y");
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedScoreSettingImpact(request)).thenReturn(false);
        when(mapper.findScoreSettingByKey(request)).thenReturn(before, after);

        basic60Service.saveScoreSetting(request, 1L, "REQ-B70-SCORE-AUDIT");

        verify(mapper).upsertScoreSetting(request, 1L);
        verify(mapper).insertChangeHistory(
                "management_item_evaluation_score_settings", "10:COLLEGE_EDU:ATTENDANCE:COL-EDU",
                "UPDATE", "evaluation_score", "10.00", "12.50", 1L, "점수 조정", "REQ-B70-SCORE-AUDIT");
    }

    @Test
    void managementItemEvaluationScoreOperationsRemainDeclaredInTheDurableOpenApiFixture() throws Exception {
        String openApi = new String(new ClassPathResource("contracts/openapi.yaml").getInputStream().readAllBytes());
        Assertions.assertThat(openApi)
                .contains("/api/admin/management-item-evaluation-score-settings:")
                .contains("operationId: listManagementItemEvaluationScoreSettings")
                .contains("/api/admin/management-item-evaluation-score-settings/save:")
                .contains("operationId: saveManagementItemEvaluationScoreSetting");
    }

    private OperationalSettingRow scoreRow(String evaluationScore, String maxScore, String activeYn) {
        return new OperationalSettingRow(
                3003L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026",
                "COURSE_GROUP", null, "ATTENDANCE", null, "COL-EDU", "사범대학", null, null, null,
                new BigDecimal(evaluationScore), new BigDecimal(maxScore), 1, activeYn, null,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "B60-SEED-003 점수 설정", 1L,
                LocalDateTime.parse("2026-09-21T09:00:00"));
    }

    private String scorePayload(Long ruleVersionId) {
        return """
                {%s"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","organizationCode":"COL-EDU","organizationName":"사범대학","evaluationScore":10.00,"maxScore":20.00,"sortOrder":1,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"점수 설정"}
                """.formatted(ruleVersionId == null ? "" : "\"ruleVersionId\":" + ruleVersionId + ",");
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

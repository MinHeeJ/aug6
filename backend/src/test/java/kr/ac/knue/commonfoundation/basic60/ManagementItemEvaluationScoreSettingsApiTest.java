package kr.ac.knue.commonfoundation.basic60;

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
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP contract tests for the FR-020 management-item evaluation-score settings flow.
 *
 * <p>These tests protect college-scoped score persistence, authorization, validation, protected
 * evaluation-result locks, and the audit transaction required for a score change.
 */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({Basic60Service.class, GlobalExceptionHandler.class})
class ManagementItemEvaluationScoreSettingsApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean Basic60Mapper mapper;

    private final CurrentUser businessAdmin = new CurrentUser(
            4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser teacher = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void saveThenListReturnsTheSavedCollegeScore() throws Exception {
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedScoreSettingImpact(any())).thenReturn(false);
        when(mapper.findScoreSettingByKey(any())).thenReturn(null, savedCollegeScore());
        when(mapper.listScoreSettings(any())).thenReturn(List.of(savedCollegeScore()));
        when(mapper.countScoreSettings(any())).thenReturn(1L);

        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-SCORE-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.organizationCode").value("COL-EDU"))
                .andExpect(jsonPath("$.data.evaluationScore").value(12.5))
                .andExpect(jsonPath("$.data.maxScore").value(20.0))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-SCORE-SAVE"));

        mockMvc.perform(get("/api/admin/management-item-evaluation-score-settings")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-SCORE-LIST")
                        .param("ruleVersionId", "10")
                        .param("targetScope", "COLLEGE_EDU")
                        .param("areaCode", "EDUCATION")
                        .param("itemCode", "LECTURE")
                        .param("managementItemCode", "ATTENDANCE")
                        .param("organizationCode", "COL-EDU")
                        .param("activeYn", "Y")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].managementItemCode")
                        .value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].organizationCode")
                        .value("COL-EDU"))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].evaluationScore").value(12.5))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].activeYn").value("Y"))
                .andExpect(jsonPath("$.data.pageSize").value(50))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-SCORE-LIST"));

        verify(mapper).upsertScoreSetting(any(), eq(4L));
        verify(mapper).insertChangeHistory(
                eq("management_item_evaluation_score_settings"),
                eq("10:COLLEGE_EDU:ATTENDANCE:COL-EDU"),
                eq("CREATE"),
                eq("evaluation_score"),
                eq(null),
                eq("12.50"),
                eq(4L),
                eq("사범대학 출석 평가점수 조정"),
                eq("REQ-B69-SCORE-SAVE"));
    }

    @Test
    void saveRejectsR01WithoutWritingACollegeScore() throws Exception {
        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(mapper, never()).upsertScoreSetting(any(), any());
    }

    @Test
    void saveReturnsFieldLevelValidationErrorWhenRuleVersionIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","organizationCode":"COL-EDU","organizationName":"사범대학","evaluationScore":12.50,"maxScore":20.00,"sortOrder":1,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"필수값 검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));

        verify(mapper, never()).upsertScoreSetting(any(), any());
    }

    @Test
    void saveReturnsConfirmedRuleLockedCodeAndLeavesCollegeScoreUntouched() throws Exception {
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(11)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_RULE_LOCKED"));

        verify(mapper, never()).upsertScoreSetting(any(), any());
    }

    @Test
    void saveReturnsConfirmedDataLockedCodeAndLeavesCollegeScoreUntouched() throws Exception {
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedScoreSettingImpact(any())).thenReturn(true);

        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-CONFIRMED-SCORE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));

        verify(mapper, never()).upsertScoreSetting(any(), any());
        verify(mapper, never()).insertChangeHistory(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private String validPayload(int ruleVersionId) {
        return """
                {"ruleVersionId":%d,"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","organizationCode":"COL-EDU","organizationName":"사범대학","evaluationScore":12.50,"maxScore":20.00,"sortOrder":1,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"사범대학 출석 평가점수 조정"}
                """.formatted(ruleVersionId);
    }

    private OperationalSettingRow savedCollegeScore() {
        return new OperationalSettingRow(
                1004L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026",
                "COURSE_GROUP", null, "ATTENDANCE", "출석관리", "COL-EDU", "사범대학", null, null, null,
                new BigDecimal("12.50"), new BigDecimal("20.00"), 1, "Y", null,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "사범대학 출석 평가점수 조정", 4L,
                LocalDateTime.parse("2026-09-21T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

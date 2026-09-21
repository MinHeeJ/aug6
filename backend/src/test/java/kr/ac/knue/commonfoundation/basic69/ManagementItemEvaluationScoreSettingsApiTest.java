package kr.ac.knue.commonfoundation.basic69;

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
import kr.ac.knue.commonfoundation.basic60.Basic60Service;
import kr.ac.knue.commonfoundation.basic60.OperationalSettingResponses;
import kr.ac.knue.commonfoundation.basic60.OperationalSettingRow;
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

/** Contract tests for management-item evaluation-score settings in the US3 red step. */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ManagementItemEvaluationScoreSettingsApiTest {
    private static final String LIST_PATH = "/api/admin/management-item-evaluation-score-settings";
    private static final String SAVE_PATH = LIST_PATH + "/save";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    Basic60Service service;

    private final CurrentUser businessAdmin = new CurrentUser(
            4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser teacher = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void getReloadsThePersistedScoreForTheSelectedCollege() throws Exception {
        when(service.listScoreSettings(any())).thenReturn(
                new OperationalSettingResponses.ManagementItemEvaluationScoreSettingSearchResponse(
                        List.of(savedRow()), 0, 20, 1));

        mockMvc.perform(get(LIST_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-SCORE-GET")
                        .param("ruleVersionId", "10")
                        .param("organizationCode", "COLLEGE-A")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].managementItemCode")
                        .value("B60_SEED_SCORE_LECTURE"))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].organizationCode")
                        .value("COLLEGE-A"))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].evaluationScore").value(92.5))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-SCORE-GET"));
    }

    @Test
    void postSavesTheScoreAndReturnsThePersistedCollegeValue() throws Exception {
        when(service.saveScoreSetting(any(), eq(4L), eq("REQ-B69-SCORE-SAVE"))).thenReturn(savedRow());

        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-SCORE-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingId").value(3001))
                .andExpect(jsonPath("$.data.organizationCode").value("COLLEGE-A"))
                .andExpect(jsonPath("$.data.evaluationScore").value(92.5))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-SCORE-SAVE"));
    }

    @Test
    void postRejectsR01BeforeChangingTheCollegeScore() throws Exception {
        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).saveScoreSetting(any(), any(), any());
    }

    @Test
    void postReportsRuleVersionIdWhenTheRequiredValueIsMissing() throws Exception {
        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'ruleVersionId')]").isNotEmpty());

        verify(service, never()).saveScoreSetting(any(), any(), any());
    }

    @Test
    void postExposesTheConfirmedRuleLockCodeWithoutSavingTheCollegeScore() throws Exception {
        when(service.saveScoreSetting(any(), eq(4L), eq("REQ-B69-SCORE-RULE-LOCK")))
                .thenThrow(new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-SCORE-RULE-LOCK")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(validPayload(11L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_RULE_LOCKED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-SCORE-RULE-LOCK"));
    }

    @Test
    void postExposesTheConfirmedDataLockCodeWithoutSavingTheCollegeScore() throws Exception {
        when(service.saveScoreSetting(any(), eq(4L), eq("REQ-B69-SCORE-DATA-LOCK")))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다."));

        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-SCORE-DATA-LOCK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
    }

    private String validPayload(Long ruleVersionId) {
        String ruleVersionField = ruleVersionId == null ? "" : "\"ruleVersionId\":%d,".formatted(ruleVersionId);
        return """
                {%s"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"B60_SEED_SCORE_LECTURE","organizationCode":"COLLEGE-A","organizationName":"A대학","evaluationScore":92.5000,"maxScore":100.0000,"sortOrder":1,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"소속대학별 평가점수 설정"}
                """.formatted(ruleVersionField);
    }

    private OperationalSettingRow savedRow() {
        return new OperationalSettingRow(3001L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026", "COURSE_GROUP", null, "B60_SEED_SCORE_LECTURE", "강의평가", "COLLEGE-A", "A대학", null, null, null, new BigDecimal("92.5000"), new BigDecimal("100.0000"), 1, "Y", null, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "소속대학별 평가점수 설정", 4L, LocalDateTime.parse("2026-09-21T19:40:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

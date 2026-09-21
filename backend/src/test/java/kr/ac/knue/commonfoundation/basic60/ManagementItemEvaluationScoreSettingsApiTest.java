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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

/** HTTP contract tests for management-item evaluation-score settings. */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ManagementItemEvaluationScoreSettingsApiTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    Basic60Service service;

    private final CurrentUser settingsAdmin = new CurrentUser(
            4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser teacher = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void saveRejectsAUserWithoutTheSettingsRole() throws Exception {
        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singlePayload(10)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).saveScoreSetting(any(), any(), any());
    }

    @Test
    void saveReportsRuleVersionIdWhenItIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", settingsAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithoutRuleVersion()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'ruleVersionId')]").isNotEmpty());

        verify(service, never()).saveScoreSetting(any(), any(), any());
    }

    @Test
    void saveReturnsTheSharedConflictEnvelopeForConfirmedDataLock() throws Exception {
        when(service.saveScoreSetting(any(), eq(4L), eq("REQ-B60-SCORE-DATA-LOCK")))
                .thenThrow(new ConflictException(
                        "평가확정 데이터에 영향을 주는 관리항목별 평가점수는 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", settingsAdmin)
                        .header("X-Request-Id", "REQ-B60-SCORE-DATA-LOCK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singlePayload(10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message")
                        .value("평가확정 데이터에 영향을 주는 관리항목별 평가점수는 수정할 수 없습니다."));
    }

    @Test
    void successfulSaveCarriesTheHistoryCorrelationRequestIdToTheTransactionBoundary() throws Exception {
        when(service.saveScoreSetting(any(), eq(4L), eq("REQ-B60-SCORE-HISTORY-001")))
                .thenReturn(attendanceScoreRow());

        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", settingsAdmin)
                        .header("X-Request-Id", "REQ-B60-SCORE-HISTORY-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singlePayload(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B60-SCORE-HISTORY-001"));

        verify(service).saveScoreSetting(any(), eq(4L), eq("REQ-B60-SCORE-HISTORY-001"));
    }

    private String singlePayload(int ruleVersionId) {
        return """
                {"ruleVersionId":%d,"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","organizationCode":"COL-EDU","organizationName":"사범대학","evaluationScore":12.50,"maxScore":20.00,"sortOrder":1,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"소속대학별 점수 조정"}
                """.formatted(ruleVersionId);
    }

    private String payloadWithoutRuleVersion() {
        return """
                {"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","organizationCode":"COL-EDU","evaluationScore":12.50,"sortOrder":1,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"소속대학별 점수 조정"}
                """;
    }

    private OperationalSettingRow attendanceScoreRow() {
        return scoreRow(3001L, "ATTENDANCE", BigDecimal.valueOf(12.5), BigDecimal.valueOf(20), 1);
    }

    private OperationalSettingRow syllabusScoreRow() {
        return scoreRow(3002L, "SYLLABUS", BigDecimal.valueOf(7.5), BigDecimal.valueOf(10), 2);
    }

    private OperationalSettingRow scoreRow(Long settingId, String managementItemCode, BigDecimal evaluationScore,
            BigDecimal maxScore, int sortOrder) {
        return new OperationalSettingRow(
                settingId, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE",
                "2026", "COURSE_GROUP", null, managementItemCode, null, "COL-EDU", "사범대학", null,
                null, null, evaluationScore, maxScore, sortOrder, "Y", null,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N",
                "소속대학별 점수 조정", 4L, LocalDateTime.parse("2026-09-21T09:00:00"));
    }
}

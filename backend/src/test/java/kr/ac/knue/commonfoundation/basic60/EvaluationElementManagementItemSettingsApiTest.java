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

/**
 * HTTP contract tests for the evaluation-element management-item settings endpoints.
 *
 * <p>These tests define the Phase 2 API behavior before the persistence implementation is
 * changed: role enforcement, validation, lock errors, and audit request correlation.</p>
 */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationElementManagementItemSettingsApiTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    Basic60Service service;

    private final CurrentUser settingsAdmin = new CurrentUser(
            4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser teacher = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listReturnsFilteredSettingsInTheApiEnvelope() throws Exception {
        when(service.listElementSettings(any())).thenReturn(
                new OperationalSettingResponses.EvaluationElementManagementItemSettingSearchResponse(
                        List.of(savedRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/evaluation-element-management-item-settings")
                        .requestAttr("currentUser", settingsAdmin)
                        .param("ruleVersionId", "10")
                        .param("elementCode", "COURSE_GROUP")
                        .param("activeYn", "Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].managementItemCode")
                        .value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].activeYn").value("Y"))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    void saveRequiresAnAuthenticatedSession() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        verify(service, never()).saveElementSetting(any(), any(), any());
    }

    @Test
    void saveRejectsAUserWithoutTheSettingsRole() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).saveElementSetting(any(), any(), any());
    }

    @Test
    void saveReportsRuleVersionIdWhenItIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", settingsAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadWithoutRuleVersion()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'ruleVersionId')]").isNotEmpty());

        verify(service, never()).saveElementSetting(any(), any(), any());
    }

    @Test
    void saveReturnsTheSharedConflictEnvelopeForConfirmedRuleLock() throws Exception {
        when(service.saveElementSetting(any(), eq(4L), eq("REQ-B60-RULE-LOCK")))
                .thenThrow(new ConflictException("확정 또는 폐기된 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", settingsAdmin)
                        .header("X-Request-Id", "REQ-B60-RULE-LOCK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(11)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void successfulSaveCarriesTheAuditRequestIdToTheTransactionBoundary() throws Exception {
        when(service.saveElementSetting(any(), eq(4L), eq("REQ-B60-AUDIT-001"))).thenReturn(savedRow());

        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", settingsAdmin)
                        .header("X-Request-Id", "REQ-B60-AUDIT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B60-AUDIT-001"));

        verify(service).saveElementSetting(any(), eq(4L), eq("REQ-B60-AUDIT-001"));
    }

    private String validPayload(int ruleVersionId) {
        return """
                {"ruleVersionId":%d,"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","managementItemName":"출석관리","sortOrder":1,"activeYn":"Y","teacherEditableYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"운영 기준 조정"}
                """.formatted(ruleVersionId);
    }

    private String validPayloadWithoutRuleVersion() {
        return """
                {"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","managementItemName":"출석관리","sortOrder":1,"activeYn":"Y","teacherEditableYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"운영 기준 조정"}
                """;
    }

    private OperationalSettingRow savedRow() {
        return new OperationalSettingRow(
                1001L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE",
                "2026", "COURSE_GROUP", null, "ATTENDANCE", "출석관리", null, null, null, null,
                null, null, null, 1, "Y", "Y", LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"), "N", "운영 기준 조정", 4L,
                LocalDateTime.parse("2026-09-21T09:00:00"));
    }
}

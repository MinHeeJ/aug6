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
 * HTTP contract tests for the FR-018 evaluation-element management-item settings flow.
 *
 * <p>These tests protect the observable endpoint contract and the audit persistence boundary.
 */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({Basic60Service.class, GlobalExceptionHandler.class})
class EvaluationElementManagementItemSettingsApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean Basic60Mapper mapper;

    private final CurrentUser businessAdmin = new CurrentUser(
            4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser teacher = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listReturnsScopedManagementItemSettingsWithRequestedPageSize() throws Exception {
        when(mapper.listElementSettings(any())).thenReturn(List.of(savedRow()));
        when(mapper.countElementSettings(any())).thenReturn(1L);

        mockMvc.perform(get("/api/admin/evaluation-element-management-item-settings")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-ELEMENT-LIST")
                        .param("ruleVersionId", "10")
                        .param("targetScope", "COLLEGE_EDU")
                        .param("areaCode", "EDUCATION")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].managementItemCode")
                        .value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].teacherEditableYn").value("Y"))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].activeYn").value("Y"))
                .andExpect(jsonPath("$.data.pageSize").value(50))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-ELEMENT-LIST"));
    }

    /**
     * Protects request correlation on a validation failure; removing request-ID propagation from
     * the error boundary must make this test fail.
     */
    @Test
    void invalidListRequestPreservesCallerRequestIdInValidationErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-element-management-item-settings")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-INVALID-PAGE-SIZE")
                        .param("pageSize", "25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-INVALID-PAGE-SIZE"));
    }

    @Test
    void saveRejectsUnauthenticatedRequestWithoutWritingASetting() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        verify(mapper, never()).upsertElementSetting(any(), any());
    }

    @Test
    void saveRejectsR01WithoutWritingASetting() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(mapper, never()).upsertElementSetting(any(), any());
    }

    @Test
    void saveReturnsFieldLevelValidationErrorWhenRuleVersionIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","managementItemName":"출석관리","sortOrder":1,"activeYn":"Y","teacherEditableYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"필수값 검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));

        verify(mapper, never()).upsertElementSetting(any(), any());
    }

    @Test
    void savePersistsDraftSettingAndRecordsAuditHistoryWithRequestId() throws Exception {
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedElementSettingImpact(any())).thenReturn(false);
        when(mapper.findElementSettingByKey(any())).thenReturn(null, savedRow());

        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-ELEMENT-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.managementItemName").value("출석관리"))
                .andExpect(jsonPath("$.data.teacherEditableYn").value("Y"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-ELEMENT-SAVE"));

        verify(mapper).upsertElementSetting(any(), eq(4L));
        verify(mapper).insertChangeHistory(
                eq("evaluation_element_management_item_settings"),
                eq("10:COLLEGE_EDU:EDUCATION:LECTURE:2026:COURSE_GROUP:ATTENDANCE"),
                eq("CREATE"),
                eq("management_item_name"),
                eq(null),
                eq("출석관리"),
                eq(4L),
                eq("항목 설정"),
                eq("REQ-B69-ELEMENT-SAVE"));
    }

    @Test
    void saveReturnsConfirmedRuleLockedCodeAndLeavesExistingSettingUntouched() throws Exception {
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-CONFIRMED-RULE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(11)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_RULE_LOCKED"));

        verify(mapper, never()).upsertElementSetting(any(), any());
    }

    @Test
    void saveReturnsConfirmedDataLockedCodeAndLeavesExistingSettingUntouched() throws Exception {
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedElementSettingImpact(any())).thenReturn(true);

        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-CONFIRMED-DATA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));

        verify(mapper, never()).upsertElementSetting(any(), any());
    }

    private String validPayload(int ruleVersionId) {
        return """
                {"ruleVersionId":%d,"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","managementItemName":"출석관리","sortOrder":1,"activeYn":"Y","teacherEditableYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"항목 설정"}
                """.formatted(ruleVersionId);
    }

    private OperationalSettingRow savedRow() {
        return new OperationalSettingRow(
                1001L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026",
                "COURSE_GROUP", null, "ATTENDANCE", "출석관리", null, null, null, null, null, null, null, 1,
                "Y", "Y", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "항목 설정", 4L,
                LocalDateTime.parse("2026-09-21T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

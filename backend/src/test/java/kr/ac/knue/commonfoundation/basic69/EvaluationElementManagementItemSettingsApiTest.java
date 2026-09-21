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

/** Contract tests for the phase-two evaluation-element management-item API. */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationElementManagementItemSettingsApiTest {
    private static final String LIST_PATH = "/api/admin/evaluation-element-management-item-settings";
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
    void getReturnsThePersistedManagementItemSettingInTheApiEnvelope() throws Exception {
        when(service.listElementSettings(any())).thenReturn(
                new OperationalSettingResponses.EvaluationElementManagementItemSettingSearchResponse(
                        List.of(savedRow()), 0, 20, 1));

        mockMvc.perform(get(LIST_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-ELEMENT-GET")
                        .param("ruleVersionId", "10")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].ruleVersionId").value(10))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].managementItemCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].teacherEditableYn").value("Y"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-ELEMENT-GET"));
    }

    @Test
    void postSavesADraftSettingAndReturnsTheSavedValue() throws Exception {
        when(service.saveElementSetting(any(), eq(4L), eq("REQ-B69-ELEMENT-SAVE"))).thenReturn(savedRow());

        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-ELEMENT-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingId").value(1001))
                .andExpect(jsonPath("$.data.activeYn").value("Y"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-ELEMENT-SAVE"));
    }

    @Test
    void postRejectsAUserWithoutR04OrR09BeforeCallingTheService() throws Exception {
        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).saveElementSetting(any(), any(), any());
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

        verify(service, never()).saveElementSetting(any(), any(), any());
    }

    @Test
    void postLeavesTheExistingSettingUntouchedWhenTheRuleVersionIsConfirmed() throws Exception {
        when(service.saveElementSetting(any(), eq(4L), eq("REQ-B69-ELEMENT-CONFIRMED")))
                .thenThrow(new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-ELEMENT-CONFIRMED")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPayload(11L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_RULE_LOCKED"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("CONFIRMED_RULE_LOCKED")));
    }

    private String validPayload(Long ruleVersionId) {
        String ruleVersionField = ruleVersionId == null ? "" : "\"ruleVersionId\":%d,".formatted(ruleVersionId);
        return """
                {%s"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","managementItemName":"출석관리","sortOrder":1,"activeYn":"Y","teacherEditableYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"항목 설정"}
                """.formatted(ruleVersionField);
    }

    private OperationalSettingRow savedRow() {
        return new OperationalSettingRow(1001L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026", "COURSE_GROUP", null, "ATTENDANCE", "출석관리", null, null, null, null, null, null, null, 1, "Y", "Y", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "항목 설정", 4L, LocalDateTime.parse("2026-09-21T19:40:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

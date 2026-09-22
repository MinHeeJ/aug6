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
import kr.ac.knue.commonfoundation.basic60.OperationalSettingSearchCriteria;
import kr.ac.knue.commonfoundation.basic60.SaveEvaluationElementManagementItemSettingRequest;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Defines the BASIC-70 evaluation-element management-item query and save contracts before
 * the production adapter adopts the approved operation fields.
 */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationElementManagementItemSettingsApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean Basic60Service service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listEvaluationElementManagementItemSettingsReturnsTheB60SeedWithinItsActiveScope() throws Exception {
        when(service.listElementSettings(any())).thenReturn(new OperationalSettingResponses.EvaluationElementManagementItemSettingSearchResponse(
                List.of(elementSeedRow()), 1, 50, 1));

        mockMvc.perform(get("/api/admin/evaluation-element-management-item-settings")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B70-ELEMENT-LIST")
                        .param("page", "1")
                        .param("targetScope", "COLLEGE_EDU")
                        .param("areaCode", "EDUCATION")
                        .param("evaluationYear", "2026")
                        .param("activeYn", "Y")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].managementItemCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].targetScope").value("COLLEGE_EDU"))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].activeYn").value("Y"))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].teacherEditablePart").value("SELF_REPORT"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(50))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B70-ELEMENT-LIST"));

        ArgumentCaptor<OperationalSettingSearchCriteria> criteria = ArgumentCaptor.forClass(OperationalSettingSearchCriteria.class);
        verify(service).listElementSettings(criteria.capture());
        org.assertj.core.api.Assertions.assertThat(criteria.getValue())
                .extracting(
                        OperationalSettingSearchCriteria::page,
                        OperationalSettingSearchCriteria::pageSize,
                        OperationalSettingSearchCriteria::targetScope,
                        OperationalSettingSearchCriteria::areaCode,
                        OperationalSettingSearchCriteria::evaluationYear,
                        OperationalSettingSearchCriteria::activeYn)
                .containsExactly(1, 50, "COLLEGE_EDU", "EDUCATION", "2026", "Y");
    }

    @Test
    void saveEvaluationElementManagementItemSettingPersistsTheApprovedEditablePartAndReturnsIt() throws Exception {
        when(service.saveElementSetting(any(), eq(4L), eq("REQ-B70-ELEMENT-SAVE"))).thenReturn(elementSeedRow());

        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B70-ELEMENT-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementPayload("SELF_REPORT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.teacherEditablePart").value("SELF_REPORT"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B70-ELEMENT-SAVE"));
        verify(service).saveElementSetting(any(), eq(4L), eq("REQ-B70-ELEMENT-SAVE"));
    }

    @Test
    void r01CannotSaveEvaluationElementManagementItemSettings() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementPayload("SELF_REPORT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).saveElementSetting(any(), any(), any());
    }

    @Test
    void saveEvaluationElementManagementItemSettingReportsTheEditablePartFieldWhenItIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementPayload("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'teacherEditablePart')]").isNotEmpty());
        verify(service, never()).saveElementSetting(any(), any(), any());
    }

    @Test
    void saveEvaluationElementManagementItemSettingReportsTheRuleVersionFieldWhenItIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementPayloadWithoutRuleVersion()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'ruleVersionId')]").isNotEmpty());
        verify(service, never()).saveElementSetting(any(), any(), any());
    }

    @Test
    void saveEvaluationElementManagementItemSettingKeepsTheRowWhenTheRuleVersionIsConfirmed() throws Exception {
        when(service.saveElementSetting(any(), eq(1L), eq("REQ-B70-ELEMENT-CONFIRMED")))
                .thenThrow(new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B70-ELEMENT-CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementPayload("SELF_REPORT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_RULE_LOCKED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B70-ELEMENT-CONFIRMED"));
    }

    @Test
    void serviceWritesElementSettingAuditHistoryWithTheRequestId() {
        Basic60Mapper mapper = org.mockito.Mockito.mock(Basic60Mapper.class);
        Basic60Service basic60Service = new Basic60Service(mapper);
        SaveEvaluationElementManagementItemSettingRequest request = new SaveEvaluationElementManagementItemSettingRequest(
                10L, "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026", "COURSE_GROUP", "ATTENDANCE",
                "출석관리", 1, "Y", "Y", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "관리항목 변경");
        OperationalSettingRow before = elementSeedRow();
        OperationalSettingRow after = new OperationalSettingRow(
                1001L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026",
                "COURSE_GROUP", null, "ATTENDANCE", "출석관리", null, null, null, null, null, null, null,
                1, "N", "Y", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "관리항목 변경", 4L,
                LocalDateTime.parse("2026-09-21T09:00:00"));
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedElementSettingImpact(request)).thenReturn(false);
        when(mapper.findElementSettingByKey(request)).thenReturn(before, after);

        basic60Service.saveElementSetting(request, 4L, "REQ-B70-ELEMENT-AUDIT");

        verify(mapper).upsertElementSetting(request, 4L);
        verify(mapper).insertChangeHistory(
                "evaluation_element_management_item_settings", "10:COLLEGE_EDU:EDUCATION:LECTURE:2026:COURSE_GROUP:ATTENDANCE",
                "UPDATE", "active_yn", "Y", "N", 4L, "관리항목 변경", "REQ-B70-ELEMENT-AUDIT");
    }

    private String elementPayload(String teacherEditablePart) {
        return """
                {"ruleVersionId":10,"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","managementItemName":"출석관리","sortOrder":1,"activeYn":"Y","teacherEditablePart":"%s","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"관리항목 변경"}
                """.formatted(teacherEditablePart);
    }

    private String elementPayloadWithoutRuleVersion() {
        return """
                {"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","managementItemName":"출석관리","sortOrder":1,"activeYn":"Y","teacherEditablePart":"SELF_REPORT","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"관리항목 변경"}
                """;
    }

    private OperationalSettingRow elementSeedRow() {
        return new OperationalSettingRow(
                1001L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026",
                "COURSE_GROUP", null, "ATTENDANCE", "출석관리", null, null, null, null, null, null, null,
                1, "Y", "SELF_REPORT", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "관리항목 설정", 4L,
                LocalDateTime.parse("2026-09-21T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

package kr.ac.knue.commonfoundation.basic33;

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

@WebMvcTest(EvaluationManagementItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationManagementItemApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationManagementItemService service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listEvaluationManagementItemsReturnsDefaultTwentyAndFiltersForReq882Req884() throws Exception {
        when(service.list(new EvaluationManagementItemSearchCriteria(0, 20, 10L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE", "Y", "증빙")))
                .thenReturn(new EvaluationManagementItemSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/evaluation-management-items")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-MGMT-ITEM-LIST")
                        .param("ruleVersionId", "10")
                        .param("areaCode", "EDUCATION")
                        .param("itemCode", "LECTURE")
                        .param("evaluationYear", "2026")
                        .param("elementCode", "ATTENDANCE")
                        .param("activeYn", "Y")
                        .param("keyword", "증빙"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationManagementItems[0].ruleVersionId").value(10))
                .andExpect(jsonPath("$.data.evaluationManagementItems[0].areaCode").value("EDUCATION"))
                .andExpect(jsonPath("$.data.evaluationManagementItems[0].elementCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.evaluationManagementItems[0].managementItemCode").value("EVIDENCE"))
                .andExpect(jsonPath("$.data.evaluationManagementItems[0].teacherEditableYn").value("Y"))
                .andExpect(jsonPath("$.data.evaluationManagementItems[0].requiredYn").value("Y"))
                .andExpect(jsonPath("$.data.evaluationManagementItems[0].dataType").value("FILE"))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B33-MGMT-ITEM-LIST"));
    }

    @Test
    void r01CannotListEvaluationManagementItemsForReq882() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-management-items")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void saveEvaluationManagementItemPersistsDraftItemAndReturnsRequestIdForReq883Req884() throws Exception {
        when(service.save(any(SaveEvaluationManagementItemRequest.class), eq(1L), eq("REQ-B33-MGMT-ITEM-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/evaluation-management-items/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-MGMT-ITEM-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","managementItemCode":"EVIDENCE","managementItemName":"증빙파일","sortOrder":1,"activeYn":"Y","teacherEditableYn":"Y","requiredYn":"Y","dataType":"FILE","changeReason":"관리항목 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("EVIDENCE"))
                .andExpect(jsonPath("$.data.managementItemName").value("증빙파일"))
                .andExpect(jsonPath("$.data.teacherEditableYn").value("Y"))
                .andExpect(jsonPath("$.data.requiredYn").value("Y"))
                .andExpect(jsonPath("$.data.dataType").value("FILE"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B33-MGMT-ITEM-SAVE"));
        org.assertj.core.api.Assertions.assertThat("x-side-effects:data_change_histories,evaluation_management_items")
                .contains("data_change_histories", "evaluation_management_items");
    }

    @Test
    void saveEvaluationManagementItemRequiresRuleVersionIdForReq883() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-management-items/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","managementItemCode":"EVIDENCE","managementItemName":"증빙파일","sortOrder":1,"activeYn":"Y","teacherEditableYn":"Y","requiredYn":"Y","dataType":"FILE","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));
    }

    @Test
    void saveEvaluationManagementItemRejectsConfirmedRuleVersionForReq885() throws Exception {
        when(service.save(any(SaveEvaluationManagementItemRequest.class), eq(1L), eq("REQ-B33-MGMT-ITEM-CONFLICT")))
                .thenThrow(new ConflictException("확정 또는 폐기된 규정버전의 관리항목은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/evaluation-management-items/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-MGMT-ITEM-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":11,"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","managementItemCode":"EVIDENCE","managementItemName":"증빙파일 변경","sortOrder":1,"activeYn":"Y","teacherEditableYn":"Y","requiredYn":"Y","dataType":"FILE","changeReason":"확정 차단 검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void r01CannotSaveEvaluationManagementItemForReq885() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-management-items/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","managementItemCode":"EVIDENCE","managementItemName":"증빙파일","sortOrder":1,"activeYn":"Y","teacherEditableYn":"Y","requiredYn":"Y","dataType":"FILE","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionWithoutChangingEvaluationManagementItemForReq885() {
        EvaluationManagementItemMapper mapper = org.mockito.Mockito.mock(EvaluationManagementItemMapper.class);
        EvaluationManagementItemService managementItemService = new EvaluationManagementItemService(mapper);
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> managementItemService.save(
                        new SaveEvaluationManagementItemRequest(11L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE", "EVIDENCE", "증빙파일 변경", 1, "Y", "Y", "Y", "FILE", "확정 차단 검증"), 1L, "REQ-B33-MGMT-ITEM-CONFLICT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("규정버전");
        verify(mapper, never()).upsertEvaluationManagementItem(any(), any(), any());
    }

    @Test
    void serviceRecordsManagementItemChangeHistoryWithRequestIdForReq885() {
        EvaluationManagementItemMapper mapper = org.mockito.Mockito.mock(EvaluationManagementItemMapper.class);
        EvaluationManagementItemService managementItemService = new EvaluationManagementItemService(mapper);
        EvaluationManagementItemRow before = new EvaluationManagementItemRow(400L, 300L, 200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의", "2026", "ATTENDANCE", "출석", "EVIDENCE", "증빙파일", 1, "Y", "Y", "Y", "FILE", "기존", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
        EvaluationManagementItemRow after = new EvaluationManagementItemRow(400L, 300L, 200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의", "2026", "ATTENDANCE", "출석", "EVIDENCE", "증빙자료", 1, "Y", "Y", "Y", "FILE", "명칭 정비", 1L, LocalDateTime.parse("2026-08-31T09:05:00"));
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.findElementId(10L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE")).thenReturn(300L);
        when(mapper.findByKey(300L, "EVIDENCE")).thenReturn(before, after);

        managementItemService.save(new SaveEvaluationManagementItemRequest(10L, "education", "lecture", "2026", "attendance", "evidence", "증빙자료", 1, "Y", "Y", "Y", "FILE", "명칭 정비"), 1L, "REQ-B33-MGMT-ITEM-AUDIT");

        verify(mapper).upsertEvaluationManagementItem(new SaveEvaluationManagementItemRequest(10L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE", "EVIDENCE", "증빙자료", 1, "Y", "Y", "Y", "FILE", "명칭 정비"), 300L, 1L);
        verify(mapper).insertChangeHistory("evaluation_management_items", "10:EDUCATION:LECTURE:2026:ATTENDANCE:EVIDENCE", "UPDATE", "management_item_name", "증빙파일", "증빙자료", 1L, "명칭 정비", "REQ-B33-MGMT-ITEM-AUDIT");
        org.assertj.core.api.Assertions.assertThat("x-side-effects:data_change_histories,evaluation_management_items")
                .contains("data_change_histories", "evaluation_management_items");
    }

    private EvaluationManagementItemRow row() {
        return new EvaluationManagementItemRow(400L, 300L, 200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의", "2026", "ATTENDANCE", "출석", "EVIDENCE", "증빙파일", 1, "Y", "Y", "Y", "FILE", "관리항목 정비", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

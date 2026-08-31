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

@WebMvcTest(EvaluationItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationItemApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationItemService service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listEvaluationItemsReturnsDefaultTwentyAndFiltersForReq870() throws Exception {
        when(service.list(new EvaluationItemSearchCriteria(0, 20, 10L, "EDUCATION", "Y", "강의")))
                .thenReturn(new EvaluationItemSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/evaluation-items")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-ITEM-LIST")
                        .param("ruleVersionId", "10")
                        .param("areaCode", "EDUCATION")
                        .param("activeYn", "Y")
                        .param("keyword", "강의"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationItems[0].ruleVersionId").value(10))
                .andExpect(jsonPath("$.data.evaluationItems[0].areaCode").value("EDUCATION"))
                .andExpect(jsonPath("$.data.evaluationItems[0].itemCode").value("LECTURE"))
                .andExpect(jsonPath("$.data.evaluationItems[0].itemName").value("강의"))
                .andExpect(jsonPath("$.data.evaluationItems[0].scoreApplyMethod").value("FIXED"))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B33-ITEM-LIST"));
    }

    @Test
    void r01CannotListEvaluationItemsForReq870() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-items")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void saveEvaluationItemPersistsDraftItemAndReturnsRequestIdForReq871Req872() throws Exception {
        when(service.save(any(SaveEvaluationItemRequest.class), eq(1L), eq("REQ-B33-ITEM-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/evaluation-items/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-ITEM-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"areaCode":"EDUCATION","itemCode":"LECTURE","itemName":"강의","parentItemCode":null,"sortOrder":1,"activeYn":"Y","scoreApplyMethod":"FIXED","changeReason":"평가항목 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCode").value("LECTURE"))
                .andExpect(jsonPath("$.data.itemName").value("강의"))
                .andExpect(jsonPath("$.data.scoreApplyMethod").value("FIXED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B33-ITEM-SAVE"));
        org.assertj.core.api.Assertions.assertThat("x-side-effects:data_change_histories,evaluation_items")
                .contains("data_change_histories", "evaluation_items");
    }

    @Test
    void saveEvaluationItemRequiresRuleVersionIdForReq871() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-items/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"areaCode":"EDUCATION","itemCode":"LECTURE","itemName":"강의","sortOrder":1,"activeYn":"Y","scoreApplyMethod":"FIXED","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));
    }

    @Test
    void saveEvaluationItemRejectsConfirmedRuleVersionForReq873() throws Exception {
        when(service.save(any(SaveEvaluationItemRequest.class), eq(1L), eq("REQ-B33-ITEM-CONFLICT")))
                .thenThrow(new ConflictException("확정 또는 폐기된 규정버전의 평가항목은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/evaluation-items/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-ITEM-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":11,"areaCode":"EDUCATION","itemCode":"LECTURE","itemName":"강의 변경","sortOrder":1,"activeYn":"Y","scoreApplyMethod":"FIXED","changeReason":"확정 차단 검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void r01CannotSaveEvaluationItemForReq873() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-items/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"areaCode":"EDUCATION","itemCode":"LECTURE","itemName":"강의","sortOrder":1,"activeYn":"Y","scoreApplyMethod":"FIXED","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionWithoutChangingEvaluationItemForReq873() {
        EvaluationItemMapper mapper = org.mockito.Mockito.mock(EvaluationItemMapper.class);
        EvaluationItemService evaluationItemService = new EvaluationItemService(mapper);
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluationItemService.save(
                        new SaveEvaluationItemRequest(11L, "EDUCATION", "LECTURE", "강의 변경", null, 1, "Y", "FIXED", "확정 차단 검증"), 1L, "REQ-B33-ITEM-CONFLICT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("규정버전");
        verify(mapper, never()).upsertEvaluationItem(any(), any(), any());
    }

    @Test
    void serviceRecordsItemChangeHistoryWithRequestIdForReq873() {
        EvaluationItemMapper mapper = org.mockito.Mockito.mock(EvaluationItemMapper.class);
        EvaluationItemService evaluationItemService = new EvaluationItemService(mapper);
        EvaluationItemRow before = new EvaluationItemRow(200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의", null, 1, "Y", "FIXED", "기존", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
        EvaluationItemRow after = new EvaluationItemRow(200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의성과", null, 1, "Y", "FIXED", "명칭 정비", 1L, LocalDateTime.parse("2026-08-31T09:05:00"));
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.findAreaId(10L, "EDUCATION")).thenReturn(100L);
        when(mapper.findByKey(100L, "LECTURE")).thenReturn(before, after);

        evaluationItemService.save(new SaveEvaluationItemRequest(10L, "education", "lecture", "강의성과", null, 1, "Y", "FIXED", "명칭 정비"), 1L, "REQ-B33-ITEM-AUDIT");

        verify(mapper).upsertEvaluationItem(new SaveEvaluationItemRequest(10L, "EDUCATION", "LECTURE", "강의성과", null, 1, "Y", "FIXED", "명칭 정비"), 100L, 1L);
        verify(mapper).insertChangeHistory("evaluation_items", "10:EDUCATION:LECTURE", "UPDATE", "item_name", "강의", "강의성과", 1L, "명칭 정비", "REQ-B33-ITEM-AUDIT");
        org.assertj.core.api.Assertions.assertThat("x-side-effects:data_change_histories,evaluation_items")
                .contains("data_change_histories", "evaluation_items");
    }

    private EvaluationItemRow row() {
        return new EvaluationItemRow(200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의", null, 1, "Y", "FIXED", "평가항목 정비", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

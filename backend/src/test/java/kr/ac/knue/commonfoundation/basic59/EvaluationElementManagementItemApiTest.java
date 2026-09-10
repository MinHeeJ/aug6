package kr.ac.knue.commonfoundation.basic59;

import static org.hamcrest.Matchers.hasItem;
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

@WebMvcTest(EvaluationElementManagementItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationElementManagementItemApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationElementManagementItemService service;

    private final CurrentUser r04 = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser r09 = new CurrentUser(9L, "admin", "E0009", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser r01 = new CurrentUser(1L, "professor1", "E0001", "교원", List.of("R01"), List.of());
    private final CurrentUser r08 = new CurrentUser(8L, "auditor", "E0008", "감사담당자", List.of("R08"), List.of());

    @Test
    void listEvaluationElementManagementItemsSupportsFiltersPaginationAndRequestIdForR04R09() throws Exception {
        when(service.list(any(EvaluationElementManagementItemSearchCriteria.class))).thenReturn(response());

        mockMvc.perform(get("/api/business/evaluation-element-management-items")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B59-FR018-LIST")
                        .param("page", "0")
                        .param("pageSize", "20")
                        .param("evaluationYear", "2026")
                        .param("areaCode", "EDUCATION")
                        .param("elementCode", "LECTURE_EVALUATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationElementManagementItems[0].settingId").value(5900101))
                .andExpect(jsonPath("$.data.evaluationElementManagementItems[0].managementItemCode").value("LECTURE_EVAL_SCORE"))
                .andExpect(jsonPath("$.data.evaluationElementManagementItems[0].teacherEditablePart").value("점수 확인 및 의견 입력"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B59-FR018-LIST"));

        mockMvc.perform(get("/api/business/evaluation-element-management-items")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void listEvaluationElementManagementItemsRejectsUnsupportedPageSize() throws Exception {
        mockMvc.perform(get("/api/business/evaluation-element-management-items")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .param("pageSize", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("pageSize")));
        verify(service, never()).list(any());
    }

    @Test
    void saveEvaluationElementManagementItemPersistsAndReturnsSavedRow() throws Exception {
        when(service.save(any(SaveEvaluationElementManagementItemRequest.class), eq(r04), eq("REQ-B59-FR018-SAVE"))).thenReturn(savedRow());

        mockMvc.perform(post("/api/business/evaluation-element-management-items/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B59-FR018-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("개선 강의평가 점수", 4L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.managementItemName").value("개선 강의평가 점수"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B59-FR018-SAVE"));
    }

    @Test
    void saveEvaluationElementManagementItemReturnsFieldErrorForMissingManagementItemCode() throws Exception {
        mockMvc.perform(post("/api/business/evaluation-element-management-items/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleVersionId\":1,\"evaluationYear\":\"2026\",\"areaCode\":\"EDUCATION\",\"elementCode\":\"LECTURE_EVALUATION\",\"managementItemName\":\"강의평가\",\"teacherEditablePart\":\"점수 확인\",\"sortOrder\":1,\"activeYn\":\"Y\",\"changeReason\":\"검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("managementItemCode")));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void saveEvaluationElementManagementItemRejectsConfirmedRuleVersionAsConflictNoChange() throws Exception {
        when(service.save(any(SaveEvaluationElementManagementItemRequest.class), eq(r04), any()))
                .thenThrow(new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/business/evaluation-element-management-items/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("구 강의평가 환산점수 변경", 5900103L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));
    }

    @Test
    void r01AndR08CannotAccessEvaluationElementManagementItems() throws Exception {
        mockMvc.perform(get("/api/business/evaluation-element-management-items").requestAttr("currentUser", r01).cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(post("/api/business/evaluation-element-management-items/save")
                        .requestAttr("currentUser", r08)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("권한 차단", 4L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionBeforePersistenceAndRecordsChangeHistoryOnSave() {
        EvaluationElementManagementItemMapper mapper = org.mockito.Mockito.mock(EvaluationElementManagementItemMapper.class);
        EvaluationElementManagementItemService elementService = new EvaluationElementManagementItemService(mapper);
        when(mapper.findRuleVersionStatus(2L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> elementService.save(validRequest(2L), r04, "REQ-B59-LOCK"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_RULE_LOCKED");
        verify(mapper, never()).upsertEvaluationElementManagementItem(any(), any(), any());

        when(mapper.findRuleVersionStatus(1L)).thenReturn("DRAFT");
        when(mapper.findByBusinessKey(1L, "2026", "EDUCATION", "LECTURE_EVALUATION", "LECTURE_EVAL_SCORE")).thenReturn(row());
        when(mapper.upsertEvaluationElementManagementItem(any(), eq(4L), eq("REQ-B59-AUDIT"))).thenReturn(savedRow());

        elementService.save(validRequest(1L), r04, "REQ-B59-AUDIT");

        verify(mapper).insertChangeHistory(eq("evaluation_element_management_item_settings"),
                eq("1:2026:EDUCATION:LECTURE_EVALUATION:LECTURE_EVAL_SCORE"), eq("UPDATE"),
                eq("setting"), any(), any(), eq(4L), eq("FR-018 저장"), eq("REQ-B59-AUDIT"));
    }

    private EvaluationElementManagementItemSearchResponse response() {
        return new EvaluationElementManagementItemSearchResponse(List.of(row()), 0, 20, 1);
    }

    private EvaluationElementManagementItemRow row() {
        return new EvaluationElementManagementItemRow(5900101L, 1L, "B33-DRAFT-2026", "DRAFT", "2026", "EDUCATION", "LECTURE_EVALUATION", "LECTURE_EVAL_SCORE", "강의평가 점수", "점수 확인 및 의견 입력", 1, "Y", "B59-SEED-001 정상 강의평가 관리항목", 9L, LocalDateTime.parse("2026-09-01T09:00:00"));
    }

    private EvaluationElementManagementItemRow savedRow() {
        return new EvaluationElementManagementItemRow(5900101L, 1L, "B33-DRAFT-2026", "DRAFT", "2026", "EDUCATION", "LECTURE_EVALUATION", "LECTURE_EVAL_SCORE", "개선 강의평가 점수", "점수 확인 및 의견 입력", 1, "Y", "FR-018 저장", 4L, LocalDateTime.parse("2026-09-10T09:00:00"));
    }

    private SaveEvaluationElementManagementItemRequest validRequest(Long ruleVersionId) {
        return new SaveEvaluationElementManagementItemRequest(ruleVersionId, "2026", "EDUCATION", "LECTURE_EVALUATION", "LECTURE_EVAL_SCORE", "개선 강의평가 점수", "점수 확인 및 의견 입력", 1, "Y", "FR-018 저장");
    }

    private String validJson(String name, Long ruleVersionId) {
        return "{\"ruleVersionId\":" + ruleVersionId + ",\"evaluationYear\":\"2026\",\"areaCode\":\"EDUCATION\",\"elementCode\":\"LECTURE_EVALUATION\",\"managementItemCode\":\"LECTURE_EVAL_SCORE\",\"managementItemName\":\"" + name + "\",\"teacherEditablePart\":\"점수 확인 및 의견 입력\",\"sortOrder\":1,\"activeYn\":\"Y\",\"changeReason\":\"FR-018 저장\"}";
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

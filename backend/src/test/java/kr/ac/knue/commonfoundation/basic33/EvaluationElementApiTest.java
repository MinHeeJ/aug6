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

@WebMvcTest(EvaluationElementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationElementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationElementService service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listEvaluationElementsReturnsDefaultTwentyAndFiltersForReq876() throws Exception {
        when(service.list(new EvaluationElementSearchCriteria(0, 20, 10L, "EDUCATION", "LECTURE", "2026", "Y", "출석")))
                .thenReturn(new EvaluationElementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/evaluation-elements")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-ELEMENT-LIST")
                        .param("ruleVersionId", "10")
                        .param("areaCode", "EDUCATION")
                        .param("itemCode", "LECTURE")
                        .param("evaluationYear", "2026")
                        .param("activeYn", "Y")
                        .param("keyword", "출석"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationElements[0].ruleVersionId").value(10))
                .andExpect(jsonPath("$.data.evaluationElements[0].areaCode").value("EDUCATION"))
                .andExpect(jsonPath("$.data.evaluationElements[0].itemCode").value("LECTURE"))
                .andExpect(jsonPath("$.data.evaluationElements[0].evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.evaluationElements[0].elementCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.evaluationElements[0].elementName").value("출석"))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B33-ELEMENT-LIST"));
    }

    @Test
    void r01CannotListEvaluationElementsForReq876() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-elements")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void saveEvaluationElementPersistsDraftElementAndReturnsRequestIdForReq877Req878() throws Exception {
        when(service.save(any(SaveEvaluationElementRequest.class), eq(1L), eq("REQ-B33-ELEMENT-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/evaluation-elements/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-ELEMENT-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","elementName":"출석","sortOrder":1,"activeYn":"Y","changeReason":"평가요소 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.elementCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.elementName").value("출석"))
                .andExpect(jsonPath("$.data.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B33-ELEMENT-SAVE"));
        org.assertj.core.api.Assertions.assertThat("x-side-effects:data_change_histories,evaluation_elements")
                .contains("data_change_histories", "evaluation_elements");
    }

    @Test
    void saveEvaluationElementRequiresRuleVersionIdForReq877() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-elements/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","elementName":"출석","sortOrder":1,"activeYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));
    }

    @Test
    void saveEvaluationElementRejectsConfirmedRuleVersionForReq879() throws Exception {
        when(service.save(any(SaveEvaluationElementRequest.class), eq(1L), eq("REQ-B33-ELEMENT-CONFLICT")))
                .thenThrow(new ConflictException("확정 또는 폐기된 규정버전의 평가요소는 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/evaluation-elements/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-ELEMENT-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":11,"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","elementName":"출석 변경","sortOrder":1,"activeYn":"Y","changeReason":"확정 차단 검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void r01CannotSaveEvaluationElementForReq879() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-elements/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","elementName":"출석","sortOrder":1,"activeYn":"Y","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionWithoutChangingEvaluationElementForReq879() {
        EvaluationElementMapper mapper = org.mockito.Mockito.mock(EvaluationElementMapper.class);
        EvaluationElementService evaluationElementService = new EvaluationElementService(mapper);
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluationElementService.save(
                        new SaveEvaluationElementRequest(11L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE", "출석 변경", 1, "Y", "확정 차단 검증"), 1L, "REQ-B33-ELEMENT-CONFLICT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("규정버전");
        verify(mapper, never()).upsertEvaluationElement(any(), any(), any());
    }

    @Test
    void serviceRecordsElementChangeHistoryWithRequestIdForReq879() {
        EvaluationElementMapper mapper = org.mockito.Mockito.mock(EvaluationElementMapper.class);
        EvaluationElementService evaluationElementService = new EvaluationElementService(mapper);
        EvaluationElementRow before = new EvaluationElementRow(300L, 200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의", "2026", "ATTENDANCE", "출석", 1, "Y", "기존", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
        EvaluationElementRow after = new EvaluationElementRow(300L, 200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의", "2026", "ATTENDANCE", "출석성과", 1, "Y", "명칭 정비", 1L, LocalDateTime.parse("2026-08-31T09:05:00"));
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.findItemId(10L, "EDUCATION", "LECTURE")).thenReturn(200L);
        when(mapper.findByKey(200L, "2026", "ATTENDANCE")).thenReturn(before, after);

        evaluationElementService.save(new SaveEvaluationElementRequest(10L, "education", "lecture", "2026", "attendance", "출석성과", 1, "Y", "명칭 정비"), 1L, "REQ-B33-ELEMENT-AUDIT");

        verify(mapper).upsertEvaluationElement(new SaveEvaluationElementRequest(10L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE", "출석성과", 1, "Y", "명칭 정비"), 200L, 1L);
        verify(mapper).insertChangeHistory("evaluation_elements", "10:EDUCATION:LECTURE:2026:ATTENDANCE", "UPDATE", "element_name", "출석", "출석성과", 1L, "명칭 정비", "REQ-B33-ELEMENT-AUDIT");
        org.assertj.core.api.Assertions.assertThat("x-side-effects:data_change_histories,evaluation_elements")
                .contains("data_change_histories", "evaluation_elements");
    }

    private EvaluationElementRow row() {
        return new EvaluationElementRow(300L, 200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의", "2026", "ATTENDANCE", "출석", 1, "Y", "평가요소 정비", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

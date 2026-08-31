package kr.ac.knue.commonfoundation.basic34;

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

@WebMvcTest(EvaluationRuleSetController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationRuleSetApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationRuleSetService service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listEvaluationRuleSetsReturnsPaginationAndFiltersForReq1017Req1018() throws Exception {
        when(service.list(new EvaluationRuleSetSearchCriteria(0, 20, 10L, "FACULTY", "교수업적", "DRAFT", "Y", "점수")))
                .thenReturn(new EvaluationRuleSetSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/evaluation-rule-sets")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-RULE-SET-LIST")
                        .param("ruleVersionId", "10")
                        .param("targetScope", "FACULTY")
                        .param("ruleSetName", "교수업적")
                        .param("ruleSetStatus", "DRAFT")
                        .param("activeYn", "Y")
                        .param("keyword", "점수"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationRuleSets[0].ruleSetId").value(910))
                .andExpect(jsonPath("$.data.evaluationRuleSets[0].ruleVersionId").value(10))
                .andExpect(jsonPath("$.data.evaluationRuleSets[0].targetScope").value("FACULTY"))
                .andExpect(jsonPath("$.data.evaluationRuleSets[0].ruleSetName").value("교수업적 기준·점수규칙"))
                .andExpect(jsonPath("$.data.evaluationRuleSets[0].ruleSetStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B34-RULE-SET-LIST"));
    }

    @Test
    void r01CannotListEvaluationRuleSetsForReq1017() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-rule-sets")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void listEvaluationRuleSetsRejectsInvalidPageSizeForReq1017() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-rule-sets")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .param("pageSize", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("pageSize"));
        verify(service, never()).list(any());
    }

    @Test
    void saveEvaluationRuleSetPersistsDraftRuleSetAndReturnsRequestIdForReq1018Req1019() throws Exception {
        when(service.save(any(SaveEvaluationRuleSetRequest.class), eq(1L), eq("REQ-B34-RULE-SET-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/evaluation-rule-sets/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-RULE-SET-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"targetScope":"FACULTY","ruleSetName":"교수업적 기준·점수규칙","ruleSetStatus":"DRAFT","activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"통합 기준 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleSetId").value(910))
                .andExpect(jsonPath("$.data.targetScope").value("FACULTY"))
                .andExpect(jsonPath("$.data.ruleSetName").value("교수업적 기준·점수규칙"))
                .andExpect(jsonPath("$.data.activeYn").value("Y"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B34-RULE-SET-SAVE"));
    }

    @Test
    void saveEvaluationRuleSetRequiresRuleVersionIdForReq1018() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-rule-sets/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetScope":"FACULTY","ruleSetName":"교수업적 기준·점수규칙","ruleSetStatus":"DRAFT","activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));
    }

    @Test
    void saveEvaluationRuleSetRejectsConfirmedRuleVersionForReq1020() throws Exception {
        when(service.save(any(SaveEvaluationRuleSetRequest.class), eq(1L), eq("REQ-B34-RULE-SET-CONFLICT")))
                .thenThrow(new ConflictException("확정 또는 폐기된 규정버전의 업적평가 기준·점수규칙은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/evaluation-rule-sets/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-RULE-SET-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":11,"targetScope":"FACULTY","ruleSetName":"교수업적 기준·점수규칙","ruleSetStatus":"DRAFT","activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"확정 차단 검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void r01CannotSaveEvaluationRuleSetForReq1023() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-rule-sets/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"targetScope":"FACULTY","ruleSetName":"교수업적 기준·점수규칙","ruleSetStatus":"DRAFT","activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionWithoutChangingEvaluationRuleSetForReq1020() {
        EvaluationRuleSetMapper mapper = org.mockito.Mockito.mock(EvaluationRuleSetMapper.class);
        EvaluationRuleSetService ruleSetService = new EvaluationRuleSetService(mapper);
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ruleSetService.save(
                        request(11L, "DRAFT"), 1L, "REQ-B34-RULE-SET-CONFLICT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("규정버전");
        verify(mapper, never()).upsertEvaluationRuleSet(any(), any());
    }

    @Test
    void serviceRecordsEvaluationRuleSetChangeHistoryWithRequestIdForReq1018() {
        EvaluationRuleSetMapper mapper = org.mockito.Mockito.mock(EvaluationRuleSetMapper.class);
        EvaluationRuleSetService ruleSetService = new EvaluationRuleSetService(mapper);
        EvaluationRuleSetRow before = row("N");
        EvaluationRuleSetRow after = row("Y");
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.findByKey(any(SaveEvaluationRuleSetRequest.class))).thenReturn(before, after);

        ruleSetService.save(request(10L, "DRAFT"), 1L, "REQ-B34-RULE-SET-AUDIT");

        verify(mapper).upsertEvaluationRuleSet(any(SaveEvaluationRuleSetRequest.class), eq(1L));
        verify(mapper).insertChangeHistory(eq("evaluation_rule_sets"), eq("10:FACULTY:교수업적 기준·점수규칙:2026-01-01:2026-12-31"), eq("UPDATE"), eq("active_yn"), eq("N"), eq("Y"), eq(1L), eq("통합 기준 정비"), eq("REQ-B34-RULE-SET-AUDIT"));
    }

    private SaveEvaluationRuleSetRequest request(Long ruleVersionId, String ruleSetStatus) {
        return new SaveEvaluationRuleSetRequest(ruleVersionId, "FACULTY", "교수업적 기준·점수규칙", ruleSetStatus, "Y",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "통합 기준 정비");
    }

    private EvaluationRuleSetRow row() {
        return row("Y");
    }

    private EvaluationRuleSetRow row(String activeYn) {
        return new EvaluationRuleSetRow(910L, 10L, "B34-DRAFT-2026", "DRAFT", "FACULTY", "교수업적 기준·점수규칙",
                "DRAFT", activeYn, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "통합 기준 정비", 1L,
                LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

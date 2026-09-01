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
import java.math.BigDecimal;
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

@WebMvcTest(EvaluationScoreController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationScoreApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationScoreService service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listEvaluationScoresReturnsPaginationAndFiltersForReq992Req993() throws Exception {
        when(service.list(new EvaluationScoreSearchCriteria(0, 20, 10L, 400L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE", "EVIDENCE", "COL-EDU", "Y", "증빙")))
                .thenReturn(new EvaluationScoreSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/evaluation-scores")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-EVAL-SCORE-LIST")
                        .param("ruleVersionId", "10")
                        .param("managementItemId", "400")
                        .param("areaCode", "EDUCATION")
                        .param("itemCode", "LECTURE")
                        .param("evaluationYear", "2026")
                        .param("elementCode", "ATTENDANCE")
                        .param("managementItemCode", "EVIDENCE")
                        .param("organizationCode", "COL-EDU")
                        .param("activeYn", "Y")
                        .param("keyword", "증빙"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationScores[0].scoreRuleId").value(700))
                .andExpect(jsonPath("$.data.evaluationScores[0].managementItemId").value(400))
                .andExpect(jsonPath("$.data.evaluationScores[0].organizationCode").value("COL-EDU"))
                .andExpect(jsonPath("$.data.evaluationScores[0].baseScore").value(10.5))
                .andExpect(jsonPath("$.data.evaluationScores[0].maxScore").value(20.0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B34-EVAL-SCORE-LIST"));
    }

    @Test
    void r01CannotListEvaluationScoresForReq992() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-scores")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void listEvaluationScoresRejectsInvalidPageSizeForReq992() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-scores")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .param("pageSize", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("pageSize"));
        verify(service, never()).list(any());
    }

    @Test
    void postEvaluationScoresPersistsDraftScoreRuleForOpenApiContract() throws Exception {
        when(service.save(any(SaveEvaluationScoreRequest.class), eq(1L), eq("REQ-B34-EVAL-SCORE-POST"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/evaluation-scores")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-EVAL-SCORE-POST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"managementItemId":400,"organizationCode":"COL-EDU","evaluationYear":"2026","baseScore":10.5,"maxScore":20.0,"effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"평가점수 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scoreRuleId").value(700))
                .andExpect(jsonPath("$.data.ruleVersionStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.baseScore").value(10.5))
                .andExpect(jsonPath("$.data.activeYn").value("Y"));
        verify(service).save(any(SaveEvaluationScoreRequest.class), eq(1L), eq("REQ-B34-EVAL-SCORE-POST"));
    }

    @Test
    void saveEvaluationScorePersistsDraftScoreAndReturnsRequestIdForReq993Req994() throws Exception {
        when(service.save(any(SaveEvaluationScoreRequest.class), eq(1L), eq("REQ-B34-EVAL-SCORE-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/evaluation-scores/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-EVAL-SCORE-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"managementItemId":400,"organizationCode":"COL-EDU","evaluationYear":"2026","baseScore":10.5,"maxScore":20.0,"effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"평가점수 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scoreRuleId").value(700))
                .andExpect(jsonPath("$.data.managementItemCode").value("EVIDENCE"))
                .andExpect(jsonPath("$.data.baseScore").value(10.5))
                .andExpect(jsonPath("$.data.maxScore").value(20.0))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B34-EVAL-SCORE-SAVE"));
    }

    @Test
    void saveEvaluationScoreRequiresRuleVersionIdForReq993() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-scores/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemId":400,"organizationCode":"COL-EDU","evaluationYear":"2026","baseScore":10.5,"maxScore":20.0,"effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));
    }

    @Test
    void saveEvaluationScoreRejectsConfirmedRuleVersionForReq999() throws Exception {
        when(service.save(any(SaveEvaluationScoreRequest.class), eq(1L), eq("REQ-B34-EVAL-SCORE-CONFLICT")))
                .thenThrow(new ConflictException("확정 또는 폐기된 규정버전의 평가점수는 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/evaluation-scores/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-EVAL-SCORE-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":11,"managementItemId":400,"organizationCode":"COL-EDU","evaluationYear":"2026","baseScore":12.0,"maxScore":20.0,"effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"확정 차단 검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void r01CannotSaveEvaluationScoreForReq993() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-scores/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"managementItemId":400,"organizationCode":"COL-EDU","evaluationYear":"2026","baseScore":10.5,"maxScore":20.0,"effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionWithoutChangingEvaluationScoreForReq999() {
        EvaluationScoreMapper mapper = org.mockito.Mockito.mock(EvaluationScoreMapper.class);
        EvaluationScoreService evaluationScoreService = new EvaluationScoreService(mapper);
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluationScoreService.save(
                        request(11L, BigDecimal.valueOf(12), BigDecimal.valueOf(20)), 1L, "REQ-B34-EVAL-SCORE-CONFLICT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("규정버전");
        verify(mapper, never()).upsertEvaluationScore(any(), any());
    }

    @Test
    void serviceRecordsScoreChangeHistoryWithRequestIdForReq994() {
        EvaluationScoreMapper mapper = org.mockito.Mockito.mock(EvaluationScoreMapper.class);
        EvaluationScoreService evaluationScoreService = new EvaluationScoreService(mapper);
        EvaluationScoreRow before = row(BigDecimal.valueOf(10.0), BigDecimal.valueOf(15.0));
        EvaluationScoreRow after = row(BigDecimal.valueOf(10.5), BigDecimal.valueOf(20.0));
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.managementItemBelongsToRuleVersion(10L, 400L)).thenReturn(true);
        when(mapper.findByKey(any(SaveEvaluationScoreRequest.class))).thenReturn(before, after);

        evaluationScoreService.save(request(10L, BigDecimal.valueOf(10.5), BigDecimal.valueOf(20.0)), 1L, "REQ-B34-EVAL-SCORE-AUDIT");

        verify(mapper).upsertEvaluationScore(any(SaveEvaluationScoreRequest.class), eq(1L));
        verify(mapper).insertChangeHistory(eq("evaluation_score_rules"), eq("10:400:COL-EDU:2026:2026-01-01:2026-12-31"), eq("UPDATE"), eq("base_score"), eq("10.0"), eq("10.5"), eq(1L), eq("평가점수 정비"), eq("REQ-B34-EVAL-SCORE-AUDIT"));
        verify(mapper).insertChangeHistory(eq("evaluation_score_rules"), eq("10:400:COL-EDU:2026:2026-01-01:2026-12-31"), eq("UPDATE"), eq("max_score"), eq("15.0"), eq("20.0"), eq(1L), eq("평가점수 정비"), eq("REQ-B34-EVAL-SCORE-AUDIT"));
    }

    private SaveEvaluationScoreRequest request(Long ruleVersionId, BigDecimal baseScore, BigDecimal maxScore) {
        return new SaveEvaluationScoreRequest(ruleVersionId, 400L, "COL-EDU", "2026", baseScore, maxScore,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "Y", "평가점수 정비");
    }

    private EvaluationScoreRow row() {
        return row(BigDecimal.valueOf(10.5), BigDecimal.valueOf(20.0));
    }

    private EvaluationScoreRow row(BigDecimal baseScore, BigDecimal maxScore) {
        return new EvaluationScoreRow(700L, 10L, "B34-DRAFT-2026", "DRAFT", 400L, "EDUCATION", "교육", "LECTURE",
                "강의", "2026", "ATTENDANCE", "출석", "EVIDENCE", "증빙파일", "COL-EDU", "사범대학",
                baseScore, maxScore, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "Y",
                "평가점수 정비", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

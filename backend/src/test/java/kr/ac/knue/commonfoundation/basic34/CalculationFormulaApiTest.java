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

@WebMvcTest(CalculationFormulaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CalculationFormulaApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean CalculationFormulaService service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listCalculationFormulasReturnsPaginationAndFiltersForReq1008Req1009() throws Exception {
        when(service.list(new CalculationFormulaSearchCriteria(0, 20, 10L, "RAW_SCORE", "FIXED_SCORE", "2026", "ROUND_HALF_UP", "Y", "원점수")))
                .thenReturn(new CalculationFormulaSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/calculation-formulas")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-FORMULA-LIST")
                        .param("ruleVersionId", "10")
                        .param("formulaCode", "RAW_SCORE")
                        .param("calculationType", "FIXED_SCORE")
                        .param("evaluationYear", "2026")
                        .param("roundingRule", "ROUND_HALF_UP")
                        .param("activeYn", "Y")
                        .param("keyword", "원점수"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.calculationFormulas[0].formulaVersionId").value(810))
                .andExpect(jsonPath("$.data.calculationFormulas[0].ruleVersionId").value(10))
                .andExpect(jsonPath("$.data.calculationFormulas[0].formulaCode").value("RAW_SCORE"))
                .andExpect(jsonPath("$.data.calculationFormulas[0].calculationType").value("FIXED_SCORE"))
                .andExpect(jsonPath("$.data.calculationFormulas[0].variableDefinition").value("{\"baseScore\":true}"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B34-FORMULA-LIST"));
    }

    @Test
    void r01CannotListCalculationFormulasForReq1008() throws Exception {
        mockMvc.perform(get("/api/admin/calculation-formulas")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void listCalculationFormulasRejectsInvalidPageSizeForReq1008() throws Exception {
        mockMvc.perform(get("/api/admin/calculation-formulas")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .param("pageSize", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("pageSize"));
        verify(service, never()).list(any());
    }

    @Test
    void postCalculationFormulasPersistsDraftFormulaWithTransactionSideEffectForOpenApiContract() throws Exception {
        when(service.save(any(SaveCalculationFormulaRequest.class), eq(1L), eq("REQ-B34-FORMULA-POST"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/calculation-formulas")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-FORMULA-POST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"formulaCode":"RAW_SCORE","calculationType":"FIXED_SCORE","variableDefinition":"{\\"baseScore\\":true}","roundingRule":"ROUND_HALF_UP","lowerBoundScore":0,"upperBoundScore":100,"evaluationYear":"2026","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"계산식 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.formulaVersionId").value(810))
                .andExpect(jsonPath("$.data.ruleVersionStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.activeYn").value("Y"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B34-FORMULA-POST"));
        verify(service).save(any(SaveCalculationFormulaRequest.class), eq(1L), eq("REQ-B34-FORMULA-POST"));
    }

    @Test
    void saveCalculationFormulaPersistsDraftFormulaAndReturnsRequestIdForReq1009Req1010() throws Exception {
        when(service.save(any(SaveCalculationFormulaRequest.class), eq(1L), eq("REQ-B34-FORMULA-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/calculation-formulas/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-FORMULA-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"formulaCode":"RAW_SCORE","calculationType":"FIXED_SCORE","variableDefinition":"{\\"baseScore\\":true}","roundingRule":"ROUND_HALF_UP","lowerBoundScore":0,"upperBoundScore":100,"evaluationYear":"2026","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"계산식 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.formulaVersionId").value(810))
                .andExpect(jsonPath("$.data.formulaCode").value("RAW_SCORE"))
                .andExpect(jsonPath("$.data.calculationType").value("FIXED_SCORE"))
                .andExpect(jsonPath("$.data.upperBoundScore").value(100))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B34-FORMULA-SAVE"));
    }

    @Test
    void saveCalculationFormulaRequiresRuleVersionIdForReq1009() throws Exception {
        mockMvc.perform(post("/api/admin/calculation-formulas/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"formulaCode":"RAW_SCORE","calculationType":"FIXED_SCORE","variableDefinition":"{}","roundingRule":"ROUND_HALF_UP","evaluationYear":"2026","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));
    }

    @Test
    void saveCalculationFormulaRejectsConfirmedRuleVersionForReq1016() throws Exception {
        when(service.save(any(SaveCalculationFormulaRequest.class), eq(1L), eq("REQ-B34-FORMULA-CONFLICT")))
                .thenThrow(new ConflictException("확정 또는 폐기된 규정버전의 계산식은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/calculation-formulas/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-FORMULA-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":11,"formulaCode":"RAW_SCORE","calculationType":"FIXED_SCORE","variableDefinition":"{}","roundingRule":"ROUND_HALF_UP","evaluationYear":"2026","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"확정 차단 검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void r01CannotSaveCalculationFormulaForReq1009() throws Exception {
        mockMvc.perform(post("/api/admin/calculation-formulas/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"formulaCode":"RAW_SCORE","calculationType":"FIXED_SCORE","variableDefinition":"{}","roundingRule":"ROUND_HALF_UP","evaluationYear":"2026","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionWithoutChangingCalculationFormulaForReq1016() {
        CalculationFormulaMapper mapper = org.mockito.Mockito.mock(CalculationFormulaMapper.class);
        CalculationFormulaService formulaService = new CalculationFormulaService(mapper);
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> formulaService.save(
                        request(11L, "ROUND_HALF_UP"), 1L, "REQ-B34-FORMULA-CONFLICT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("규정버전");
        verify(mapper, never()).upsertCalculationFormula(any(), any());
    }

    @Test
    void serviceRecordsCalculationFormulaChangeHistoryWithRequestIdForReq1010() {
        CalculationFormulaMapper mapper = org.mockito.Mockito.mock(CalculationFormulaMapper.class);
        CalculationFormulaService formulaService = new CalculationFormulaService(mapper);
        CalculationFormulaRow before = row("ROUND_DOWN");
        CalculationFormulaRow after = row("ROUND_HALF_UP");
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.findByKey(any(SaveCalculationFormulaRequest.class))).thenReturn(before, after);

        formulaService.save(request(10L, "ROUND_HALF_UP"), 1L, "REQ-B34-FORMULA-AUDIT");

        verify(mapper).upsertCalculationFormula(any(SaveCalculationFormulaRequest.class), eq(1L));
        verify(mapper).insertChangeHistory(eq("calculation_formula_versions"), eq("10:RAW_SCORE:2026:2026-01-01:2026-12-31"), eq("UPDATE"), eq("rounding_rule"), eq("ROUND_DOWN"), eq("ROUND_HALF_UP"), eq(1L), eq("계산식 정비"), eq("REQ-B34-FORMULA-AUDIT"));
    }

    private SaveCalculationFormulaRequest request(Long ruleVersionId, String roundingRule) {
        return new SaveCalculationFormulaRequest(ruleVersionId, "RAW_SCORE", "FIXED_SCORE", "{\"baseScore\":true}", roundingRule,
                BigDecimal.ZERO, BigDecimal.valueOf(100), "2026", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "Y", "계산식 정비");
    }

    private CalculationFormulaRow row() {
        return row("ROUND_HALF_UP");
    }

    private CalculationFormulaRow row(String roundingRule) {
        return new CalculationFormulaRow(810L, 10L, "B34-DRAFT-2026", "DRAFT", "RAW_SCORE", "FIXED_SCORE", "정액배점",
                "{\"baseScore\":true}", roundingRule, BigDecimal.ZERO, BigDecimal.valueOf(100), "2026",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "Y", "계산식 정비", 1L,
                LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

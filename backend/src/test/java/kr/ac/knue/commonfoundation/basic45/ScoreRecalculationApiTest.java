package kr.ac.knue.commonfoundation.basic45;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
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

@WebMvcTest(ScoreRecalculationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ScoreRecalculationApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ScoreRecalculationService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void previewReturnsTargetConditionFormulaVersionAndBeforeAfterScoresForReq1500() throws Exception {
        when(service.preview(new ScoreRecalculationSearchCriteria(0, 20, "2026", "EDUCATION", "1", "7")))
                .thenReturn(new ScoreRecalculationPreviewResponse(List.of(
                        new ScoreRecalculationTarget(45001L, "2026", "EDUCATION", 1L, 43001L,
                                "B45-GENERATION-20260903-000001", 7L, 3L, "FIXED-2026",
                                new BigDecimal("8.00"), new BigDecimal("10.00"), 2, "인증", "교육 평가자료")),
                        0, 20, 1, 1));

        mockMvc.perform(get("/api/business/score-recalculations/preview")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "EDUCATION")
                        .param("targetUserId", "1")
                        .param("formulaVersionId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targets[0].evaluationMaterialId").value(45001))
                .andExpect(jsonPath("$.data.targets[0].formulaVersionId").value(7))
                .andExpect(jsonPath("$.data.targets[0].beforeScore").value(8.00))
                .andExpect(jsonPath("$.data.targets[0].afterScore").value(10.00))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void createRecalculationReturnsBatchIdAndGenerationSummaryForReq1501() throws Exception {
        when(service.recalculate(any(EvaluationBatchActionRequest.class), any()))
                .thenReturn(new ScoreRecalculationResult("B45-RECALCULATION-20260903-000002", "REQ-B45-RECALC-TEST", 2, 2, 0));

        mockMvc.perform(post("/api/business/score-recalculations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"areaCode\":\"EDUCATION\",\"targetUserId\":\"1\",\"formulaVersionId\":\"7\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchId").value("B45-RECALCULATION-20260903-000002"))
                .andExpect(jsonPath("$.data.targetCount").value(2))
                .andExpect(jsonPath("$.data.recalculatedCount").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B45-RECALC-TEST"));
    }

    @Test
    void createRecalculationRequiresFormulaVersionAndBlocksNonR09WithoutSideEffectsForReq1501() throws Exception {
        mockMvc.perform(post("/api/business/score-recalculations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'formulaVersionId')]").exists());

        mockMvc.perform(post("/api/business/score-recalculations")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"formulaVersionId\":\"7\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).recalculate(any(), any());
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

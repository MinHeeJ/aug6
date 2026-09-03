package kr.ac.knue.commonfoundation.basic46;

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
import java.time.LocalDateTime;
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
    void listScoreRecalculationsReturnsBeforeAfterFormulaVersionAndPagingForReq1480Req1482() throws Exception {
        when(service.list(new ScoreRecalculationSearchCriteria(0, 20, "2026", "RESEARCH_CREATION", 2L)))
                .thenReturn(new ScoreRecalculationSearchResponse(
                        List.of(row(460001L, "CERTIFIED", null)),
                        0,
                        20,
                        1));

        mockMvc.perform(get("/api/business/score-recalculations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "RESEARCH_CREATION")
                        .param("targetUserId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recalculations[0].evaluationMaterialId").value(460001))
                .andExpect(jsonPath("$.data.recalculations[0].previousScore").value(10.0))
                .andExpect(jsonPath("$.data.recalculations[0].recalculatedScore").value(12.0))
                .andExpect(jsonPath("$.data.recalculations[0].formulaVersionId").value(320001))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/business/score-recalculations")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new ScoreRecalculationSearchCriteria(0, 20, null, null, null));
    }

    @Test
    void createScoreRecalculationReturnsBatchIdGenerationCountsAndRequestIdForReq1481Req1483() throws Exception {
        when(service.create(any(ScoreRecalculationRequest.class), eq(1L)))
                .thenReturn(new ScoreRecalculationResult("B46-RECALC-REQ-001", "2026", "RESEARCH_CREATION",
                        2L, "320001", 2, 1, 0, 1, "REQ-B46-RECALC-001"));

        mockMvc.perform(post("/api/business/score-recalculations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"areaCode\":\"RESEARCH_CREATION\",\"targetUserId\":2,\"formulaVersionId\":\"320001\",\"selectionReason\":\"기본 규정버전 재계산\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recalculationBatchId").value("B46-RECALC-REQ-001"))
                .andExpect(jsonPath("$.data.formulaVersionId").value("320001"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.excludedCount").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B46-RECALC-001"));
    }

    @Test
    void createScoreRecalculationRequiresFormulaVersionAndSelectionReasonAndBlocksNonR09ForReq1483Req1484() throws Exception {
        mockMvc.perform(post("/api/business/score-recalculations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'evaluationYear')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'areaCode')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'formulaVersionId')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'selectionReason')]").exists());

        mockMvc.perform(post("/api/business/score-recalculations")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"areaCode\":\"RESEARCH_CREATION\",\"formulaVersionId\":\"320001\",\"selectionReason\":\"권한 실패\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any(), eq(2L));
    }

    private ScoreRecalculationRow row(Long materialId, String materialStatus, String excludedReason) {
        return new ScoreRecalculationRow(materialId, "2026", "RESEARCH_CREATION", "KNUE-DEPT-COMP", 2L,
                846001L, materialStatus, BigDecimal.valueOf(10.00), BigDecimal.valueOf(12.00), 320001L,
                2, "B46-RECALC-REQ-001", "기본 규정버전 재계산", excludedReason,
                LocalDateTime.parse("2026-09-03T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

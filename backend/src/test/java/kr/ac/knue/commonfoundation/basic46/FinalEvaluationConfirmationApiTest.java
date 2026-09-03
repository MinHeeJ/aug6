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

@WebMvcTest(FinalEvaluationConfirmationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FinalEvaluationConfirmationApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean FinalEvaluationConfirmationService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listFinalEvaluationConfirmationsReturnsStatusSnapshotAndPagingForReq1485() throws Exception {
        when(service.list(new FinalEvaluationConfirmationSearchCriteria(0, 20, "2026", 2L, "CERTIFIED")))
                .thenReturn(new FinalEvaluationConfirmationSearchResponse(
                        List.of(row("CERTIFIED", null)), 0, 20, 1));

        mockMvc.perform(get("/api/business/final-evaluation-confirmations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("targetUserId", "2")
                        .param("finalStatus", "CERTIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.confirmations[0].targetUserId").value(2))
                .andExpect(jsonPath("$.data.confirmations[0].finalScore").value(12.0))
                .andExpect(jsonPath("$.data.confirmations[0].latestRecalculationBatchId").value("B46-BATCH-RECALC-001"))
                .andExpect(jsonPath("$.data.confirmations[0].finalStatus").value("CERTIFIED"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/business/final-evaluation-confirmations")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new FinalEvaluationConfirmationSearchCriteria(0, 20, null, null, null));
    }

    @Test
    void confirmTransitionReturnsBatchIdSnapshotAndRequestIdForReq1486Req1490() throws Exception {
        when(service.transition(eq(2L), any(FinalEvaluationTransitionRequest.class), eq(1L)))
                .thenReturn(new FinalEvaluationTransitionResult("B46-FINAL-001", 2L, "2026", "CONFIRM",
                        "EVALUATION_CONFIRMED", 2, 2, 0, 0, "B46-SNAPSHOT-001", "REQ-B46-FINAL-001"));

        mockMvc.perform(post("/api/business/final-evaluation-confirmations/2/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"CONFIRM\",\"evaluationYear\":\"2026\",\"reason\":\"최종평가 확정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalizationBatchId").value("B46-FINAL-001"))
                .andExpect(jsonPath("$.data.finalStatus").value("EVALUATION_CONFIRMED"))
                .andExpect(jsonPath("$.data.snapshotRef").value("B46-SNAPSHOT-001"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B46-FINAL-001"));
    }

    @Test
    void cancelTransitionRequiresCancelReasonAndBlocksNonR09ForReq1487Req1488Req1489() throws Exception {
        mockMvc.perform(post("/api/business/final-evaluation-confirmations/2/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"CANCEL\",\"evaluationYear\":\"2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'cancelReason')]").exists());

        mockMvc.perform(post("/api/business/final-evaluation-confirmations/2/transition")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"CONFIRM\",\"evaluationYear\":\"2026\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).transition(eq(2L), any(), eq(2L));
    }

    private FinalEvaluationConfirmationRow row(String finalStatus, String cancelReason) {
        return new FinalEvaluationConfirmationRow(2L, "2026", BigDecimal.valueOf(12.00), "B46-BATCH-RECALC-001",
                "SUCCESS", finalStatus, 1L, LocalDateTime.parse("2026-09-03T09:00:00"), null, null,
                cancelReason, "B46-SNAPSHOT-001", 2, 0);
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

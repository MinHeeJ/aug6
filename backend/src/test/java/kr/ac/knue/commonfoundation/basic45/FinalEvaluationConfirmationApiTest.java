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

@WebMvcTest(FinalEvaluationConfirmationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FinalEvaluationConfirmationApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean FinalEvaluationConfirmationService service;

    private final CurrentUser collegeStaff = new CurrentUser(4L, "college", "E0004", "단과대학담당자", List.of("R04"), List.of());
    private final CurrentUser auditor = new CurrentUser(8L, "auditor", "E0008", "점수산출감사자", List.of("R08"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listReturnsFinalEvaluationStatusConfirmerTimestampAndCancelReasonForReq1518() throws Exception {
        when(service.list(new FinalEvaluationConfirmationSearchCriteria(0, 20, "2026", "EDUCATION", "1", "평가확정")))
                .thenReturn(new FinalEvaluationConfirmationListResponse(List.of(
                        new FinalEvaluationConfirmationTarget(1L, "2026", "EDUCATION", "교육학과", "홍길동", "평가확정",
                                4L, "단과대학담당자", "2026-09-03T09:00:00", null, null, null, null,
                                2, new BigDecimal("20.00"))), 0, 20, 1));

        mockMvc.perform(get("/api/business/final-evaluation-confirmations")
                        .requestAttr("currentUser", collegeStaff)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "EDUCATION")
                        .param("targetUserId", "1")
                        .param("confirmationStatus", "평가확정"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.confirmations[0].targetId").value(1))
                .andExpect(jsonPath("$.data.confirmations[0].confirmationStatus").value("평가확정"))
                .andExpect(jsonPath("$.data.confirmations[0].confirmedBy").value(4))
                .andExpect(jsonPath("$.data.confirmations[0].confirmedAt").value("2026-09-03T09:00:00"))
                .andExpect(jsonPath("$.data.confirmations[0].cancelReason").doesNotExist())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void confirmAllowsOnlyCollegeStaffRoleAndReturnsStateTransitionBatchForReq1519() throws Exception {
        when(service.confirm(1L, "2026", 4L))
                .thenReturn(new FinalEvaluationConfirmationResult("B45-CONFIRMATION-20260903-000002", "REQ-B45-CONFIRM-TEST", 1L, "인증", "평가확정", 2));

        mockMvc.perform(post("/api/business/final-evaluation-confirmations/1/confirm")
                        .requestAttr("currentUser", collegeStaff)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetId").value(1))
                .andExpect(jsonPath("$.data.previousStatus").value("인증"))
                .andExpect(jsonPath("$.data.nextStatus").value("평가확정"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B45-CONFIRM-TEST"));

        mockMvc.perform(post("/api/business/final-evaluation-confirmations/1/confirm")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).confirm(1L, "2026", 2L);
    }

    @Test
    void cancelRequiresCancelAuthorityAndCancelReasonForReq1520() throws Exception {
        mockMvc.perform(post("/api/business/final-evaluation-confirmations/1/cancel")
                        .requestAttr("currentUser", auditor)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'cancelReason')]").exists());

        when(service.cancel(1L, "2026", "이의신청 인용", 8L))
                .thenReturn(new FinalEvaluationConfirmationResult("B45-CONFIRMATION-20260903-000003", "REQ-B45-CANCEL-TEST", 1L, "평가확정", "인증", 2));
        mockMvc.perform(post("/api/business/final-evaluation-confirmations/1/cancel")
                        .requestAttr("currentUser", auditor)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"cancelReason\":\"이의신청 인용\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousStatus").value("평가확정"))
                .andExpect(jsonPath("$.data.nextStatus").value("인증"));

        mockMvc.perform(post("/api/business/final-evaluation-confirmations/1/cancel")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"cancelReason\":\"이의신청 인용\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).cancel(1L, "2026", "이의신청 인용", 2L);
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

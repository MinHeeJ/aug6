package kr.ac.knue.commonfoundation.basic43;

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

@WebMvcTest(GrantPaymentApprovalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GrantPaymentApprovalApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean GrantPaymentApprovalService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listGrantPaymentApprovalsSupportsPaginationFiltersAmountsAccountLinkedAchievementAndR09OnlyForReq1347Req1312() throws Exception {
        when(service.list(new GrantPaymentApprovalSearchCriteria(0, 20, "2026", "SUBMITTED", "9201")))
                .thenReturn(new GrantPaymentApprovalSearchResponse(List.of(row("APPROVED", "CERTIFIED")), 0, 20, 1));

        mockMvc.perform(get("/api/business/grant-payment-approvals")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("approvalStatus", "SUBMITTED")
                        .param("applicantName", "9201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.approvals[0].grantApplicationId").value(9201))
                .andExpect(jsonPath("$.data.approvals[0].requestedAmountSnapshot").value(100000.00))
                .andExpect(jsonPath("$.data.approvals[0].paymentAmountSnapshot").value(90000.00))
                .andExpect(jsonPath("$.data.approvals[0].accountSnapshotRef").value("ACCOUNT-SNAPSHOT-9201"))
                .andExpect(jsonPath("$.data.approvals[0].linkedAchievementId").value(9101))
                .andExpect(jsonPath("$.data.size").value(20));

        mockMvc.perform(get("/api/business/grant-payment-approvals")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new GrantPaymentApprovalSearchCriteria(0, 20, null, null, null));
    }

    @Test
    void transitionApprovePersistsStatusProcessorTimestampAndHistoryForReq1348() throws Exception {
        when(service.transition(eq(9201L), any(BusinessTransitionRequest.class), eq(1L)))
                .thenReturn(row("APPROVED", "CERTIFIED"));

        mockMvc.perform(post("/api/business/grant-payment-approvals/9201/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"APPROVE\",\"opinion\":\"지급 승인\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.processedBy").value(1))
                .andExpect(jsonPath("$.data.processedAt").isString());
    }

    @Test
    void transitionRejectRequiresReasonAndOpinionForReq1348Req1314() throws Exception {
        mockMvc.perform(post("/api/business/grant-payment-approvals/9201/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REJECT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'reasonCode')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'opinion')]").exists());
    }

    @Test
    void transitionBlocksNonR09AndOutOfScopeBusinessConflictWithoutSideEffectsForReq1349() throws Exception {
        mockMvc.perform(post("/api/business/grant-payment-approvals/9201/transition")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"APPROVE\",\"opinion\":\"승인\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).transition(any(), any(), any());

        when(service.transition(eq(9201L), any(BusinessTransitionRequest.class), eq(1L)))
                .thenThrow(new ConflictException("지급승인 처리 권한 또는 데이터 범위가 없습니다."));
        mockMvc.perform(post("/api/business/grant-payment-approvals/9201/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"APPROVE\",\"opinion\":\"승인\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void transitionAllowsOnlyApproveRejectCancelApprovalForReq1352() throws Exception {
        mockMvc.perform(post("/api/business/grant-payment-approvals/9201/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PAYMENT_CONFIRM\",\"opinion\":\"범위 밖\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'actionType')]").exists());
    }

    private GrantPaymentApprovalRow row(String approvalStatus, String nextStatus) {
        return new GrantPaymentApprovalRow(701L, 9201L, 9101L, "2026", approvalStatus,
                "SUBMITTED", nextStatus, new BigDecimal("100000.00"), new BigDecimal("90000.00"),
                "ACCOUNT-SNAPSHOT-9201", null, "지급 승인", 1L,
                LocalDateTime.parse("2026-09-02T09:00:00"), "지급승인 처리");
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

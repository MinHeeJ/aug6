package kr.ac.knue.commonfoundation.basic43;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

class GrantPaymentApprovalServiceTest {
    @Test
    void transitionApproveInsertsApprovalHistoryWithoutFinancialSideEffectForReq1348Req1351() {
        GrantPaymentApprovalMapper mapper = org.mockito.Mockito.mock(GrantPaymentApprovalMapper.class);
        GrantPaymentApprovalService service = new GrantPaymentApprovalService(mapper);
        GrantPaymentApprovalRow current = row("REJECTED", "SUBMITTED");
        GrantPaymentApprovalRow inserted = row("APPROVED", "CERTIFIED");
        when(mapper.findLatestByGrantApplicationId(9201L)).thenReturn(current);
        when(mapper.paymentScopeExists(9201L, 1L)).thenReturn(1);
        when(mapper.transitionAllowed("SUBMITTED", "CERTIFIED")).thenReturn(1);
        when(mapper.insertTransition(eq(9201L), eq(9101L), eq("2026"), eq("APPROVED"),
                eq("SUBMITTED"), eq("CERTIFIED"), eq(new BigDecimal("100000.00")),
                eq(new BigDecimal("90000.00")), eq("ACCOUNT-SNAPSHOT-9201"), eq(null),
                eq("승인"), eq(1L), eq("지급승인 처리"))).thenReturn(inserted);

        GrantPaymentApprovalRow result = service.transition(9201L,
                new BusinessTransitionRequest("APPROVE", null, "승인", null), 1L);

        assertThat(result.approvalStatus()).isEqualTo("APPROVED");
        verify(mapper).findLatestByGrantApplicationId(9201L);
        verify(mapper).paymentScopeExists(9201L, 1L);
        verify(mapper).transitionAllowed("SUBMITTED", "CERTIFIED");
        verify(mapper).insertTransition(eq(9201L), eq(9101L), eq("2026"), eq("APPROVED"),
                eq("SUBMITTED"), eq("CERTIFIED"), eq(new BigDecimal("100000.00")),
                eq(new BigDecimal("90000.00")), eq("ACCOUNT-SNAPSHOT-9201"), eq(null),
                eq("승인"), eq(1L), eq("지급승인 처리"));
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void transitionRejectRequiresReasonAndOpinionBeforeMapperSideEffectForReq1348Req1314() {
        GrantPaymentApprovalMapper mapper = org.mockito.Mockito.mock(GrantPaymentApprovalMapper.class);
        GrantPaymentApprovalService service = new GrantPaymentApprovalService(mapper);

        assertThatThrownBy(() -> service.transition(9201L, new BusinessTransitionRequest("REJECT", null, "", null), 1L))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insertTransition(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void transitionCancelApprovalKeepsApplicationAndAchievementReadonlyForReq1350Req1352() {
        GrantPaymentApprovalMapper mapper = org.mockito.Mockito.mock(GrantPaymentApprovalMapper.class);
        GrantPaymentApprovalService service = new GrantPaymentApprovalService(mapper);
        when(mapper.findLatestByGrantApplicationId(9201L)).thenReturn(row("APPROVED", "CERTIFIED"));
        when(mapper.paymentScopeExists(9201L, 1L)).thenReturn(1);
        when(mapper.transitionAllowed("CERTIFIED", "SUBMITTED")).thenReturn(1);
        when(mapper.insertTransition(eq(9201L), eq(9101L), eq("2026"), eq("APPROVAL_CANCELLED"),
                eq("CERTIFIED"), eq("SUBMITTED"), eq(new BigDecimal("100000.00")),
                eq(new BigDecimal("90000.00")), eq("ACCOUNT-SNAPSHOT-9201"), eq(null),
                eq("승인취소"), eq(1L), eq("지급승인 처리")))
                .thenReturn(row("APPROVAL_CANCELLED", "SUBMITTED"));

        GrantPaymentApprovalRow result = service.transition(9201L,
                new BusinessTransitionRequest("CANCEL_APPROVAL", null, "승인취소", null), 1L);

        assertThat(result.approvalStatus()).isEqualTo("APPROVAL_CANCELLED");
        verify(mapper).findLatestByGrantApplicationId(9201L);
        verify(mapper).paymentScopeExists(9201L, 1L);
        verify(mapper).transitionAllowed("CERTIFIED", "SUBMITTED");
        verify(mapper).insertTransition(eq(9201L), eq(9101L), eq("2026"), eq("APPROVAL_CANCELLED"),
                eq("CERTIFIED"), eq("SUBMITTED"), eq(new BigDecimal("100000.00")),
                eq(new BigDecimal("90000.00")), eq("ACCOUNT-SNAPSHOT-9201"), eq(null),
                eq("승인취소"), eq(1L), eq("지급승인 처리"));
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void transitionOutOfScopeKeepsApprovalRowsUnchangedForReq1349() {
        GrantPaymentApprovalMapper mapper = org.mockito.Mockito.mock(GrantPaymentApprovalMapper.class);
        GrantPaymentApprovalService service = new GrantPaymentApprovalService(mapper);
        when(mapper.findLatestByGrantApplicationId(9201L)).thenReturn(row("REJECTED", "SUBMITTED"));
        when(mapper.paymentScopeExists(9201L, 1L)).thenReturn(0);

        assertThatThrownBy(() -> service.transition(9201L, new BusinessTransitionRequest("APPROVE", null, "승인", null), 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("지급승인 처리 권한");
        verify(mapper, never()).insertTransition(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private GrantPaymentApprovalRow row(String approvalStatus, String nextStatus) {
        return new GrantPaymentApprovalRow(701L, 9201L, 9101L, "2026", approvalStatus,
                "SUBMITTED", nextStatus, new BigDecimal("100000.00"), new BigDecimal("90000.00"),
                "ACCOUNT-SNAPSHOT-9201", null, "승인", 1L,
                LocalDateTime.parse("2026-09-02T09:00:00"), "지급승인 처리");
    }
}

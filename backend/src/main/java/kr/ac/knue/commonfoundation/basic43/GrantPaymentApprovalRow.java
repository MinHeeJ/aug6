package kr.ac.knue.commonfoundation.basic43;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GrantPaymentApprovalRow(
        Long approvalId,
        Long grantApplicationId,
        Long linkedAchievementId,
        String evaluationYear,
        String approvalStatus,
        String previousStatus,
        String nextStatus,
        BigDecimal requestedAmountSnapshot,
        BigDecimal paymentAmountSnapshot,
        String accountSnapshotRef,
        String reasonCode,
        String opinion,
        Long processedBy,
        LocalDateTime processedAt,
        String changeReason) {
}

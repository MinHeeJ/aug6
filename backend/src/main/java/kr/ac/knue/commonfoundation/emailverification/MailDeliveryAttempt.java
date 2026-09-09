package kr.ac.knue.commonfoundation.emailverification;

import java.time.LocalDateTime;

public record MailDeliveryAttempt(
        Long attemptId,
        Long userId,
        String normalizedEmail,
        String deliveryStatus,
        String diagnosticCategory,
        String diagnosticMessage,
        String retryEligibleYn,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

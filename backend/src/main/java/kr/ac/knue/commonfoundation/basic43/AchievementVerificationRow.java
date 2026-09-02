package kr.ac.knue.commonfoundation.basic43;

import java.time.LocalDateTime;

public record AchievementVerificationRow(
        Long verificationId,
        Long achievementId,
        String evaluationYear,
        Long handlerUserId,
        String actionType,
        String previousStatus,
        String nextStatus,
        String opinion,
        String evidenceRef,
        String reasonCode,
        Long processedBy,
        LocalDateTime processedAt,
        String changeReason) {
}

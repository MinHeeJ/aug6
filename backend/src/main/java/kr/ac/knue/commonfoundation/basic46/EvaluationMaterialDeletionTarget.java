package kr.ac.knue.commonfoundation.basic46;

import java.time.LocalDateTime;

public record EvaluationMaterialDeletionTarget(
        Long evaluationMaterialId,
        String evaluationYear,
        String areaCode,
        String organizationCode,
        Long targetUserId,
        Long sourceAchievementId,
        String generationBatchId,
        String finalStatus,
        boolean canDelete,
        String excludedReason,
        LocalDateTime createdAt) {
}

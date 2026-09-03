package kr.ac.knue.commonfoundation.basic46;

import java.time.LocalDateTime;

public record EvaluationMaterialGenerationTarget(
        Long sourceAchievementId,
        String evaluationYear,
        String areaCode,
        String organizationCode,
        Long targetUserId,
        String sourceStatus,
        String generationStatus,
        String generationBatchId,
        LocalDateTime lastProcessedAt,
        String excludedReason) {
}

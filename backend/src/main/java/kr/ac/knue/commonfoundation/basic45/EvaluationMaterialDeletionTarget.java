package kr.ac.knue.commonfoundation.basic45;

public record EvaluationMaterialDeletionTarget(
        Long evaluationMaterialId,
        String evaluationYear,
        String areaCode,
        Long targetUserId,
        Long sourceAchievementId,
        String generationBatchId,
        String materialStatus,
        String materialOrigin,
        String achievementTitle) {
}

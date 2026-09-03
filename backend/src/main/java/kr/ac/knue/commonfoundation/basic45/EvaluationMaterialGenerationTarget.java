package kr.ac.knue.commonfoundation.basic45;

public record EvaluationMaterialGenerationTarget(
        Long sourceAchievementId,
        String evaluationYear,
        String areaCode,
        String organizationCode,
        Long targetUserId,
        String sourceStatus,
        String achievementType,
        String achievementTitle,
        String generationStatus) {
}

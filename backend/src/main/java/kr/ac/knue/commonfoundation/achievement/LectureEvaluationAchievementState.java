package kr.ac.knue.commonfoundation.achievement;

/**
 * Projection used by the common guard and transition service for a lecture-evaluation achievement.
 */
public record LectureEvaluationAchievementState(
        Long achievementId,
        Long ownerUserId,
        String evaluationYear,
        String organizationCode,
        String certificationStatus) {
}

package kr.ac.knue.commonfoundation.achievement;

/**
 * Represents one validated status-history entry to be persisted in the same transaction as a
 * future education-achievement state change.
 */
public record EducationAchievementTransition(
        String previousStatus,
        String nextStatus,
        String actionType,
        String reasonCode,
        String opinion
) {
}

package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDateTime;

/**
 * Immutable status-history payload prepared only after a permitted education-achievement transition is validated.
 */
public record EducationAchievementStatusTransition(
        String achievementType,
        Long achievementId,
        String previousStatus,
        String nextStatus,
        String actionType,
        String reasonCode,
        String opinion,
        Long processedBy,
        LocalDateTime processedAt) {
}

package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDateTime;

/**
 * Immutable status-transition audit payload written with the owning achievement mutation.
 */
public record EducationAchievementStatusTransition(
        String achievementType,
        Long achievementId,
        EducationAchievementStatus previousStatus,
        EducationAchievementStatus nextStatus,
        String transitionReason,
        Long processedBy,
        LocalDateTime processedAt,
        String requestId) {
}

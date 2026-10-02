package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDateTime;

/**
 * Value object matching the status-history rows that the achievement save
 * transaction persists in a later vertical slice.
 */
public record EducationAchievementStatusHistory(
        String achievementType,
        Long achievementId,
        EducationAchievementStatus previousStatus,
        EducationAchievementStatus nextStatus,
        String actionType,
        String reasonCode,
        String opinion,
        Long processedBy,
        LocalDateTime processedAt) {
}

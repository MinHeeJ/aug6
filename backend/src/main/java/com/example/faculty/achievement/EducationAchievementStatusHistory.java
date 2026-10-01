package com.example.faculty.achievement;

import java.time.LocalDateTime;

/**
 * Represents the immutable data that must be persisted for an accepted education-achievement state change.
 */
public record EducationAchievementStatusHistory(
        String achievementType,
        Long achievementId,
        String previousStatus,
        String nextStatus,
        String actionType,
        String reasonCode,
        String opinion,
        Long processedBy,
        LocalDateTime processedAt
) {
}

package com.example.faculty.achievement;

/**
 * Captures one requested certification-state change before it is recorded in the education status history.
 */
public record EducationAchievementTransitionRequest(
        String achievementType,
        Long achievementId,
        String previousStatus,
        String nextStatus,
        String actionType,
        String reasonCode,
        String opinion,
        Long processedBy
) {
}

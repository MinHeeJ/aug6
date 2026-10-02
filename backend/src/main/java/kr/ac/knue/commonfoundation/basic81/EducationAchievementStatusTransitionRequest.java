package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDateTime;

/**
 * Captures one requested status transition before the later persistence slice
 * appends it to education_achievement_status_histories.
 */
public record EducationAchievementStatusTransitionRequest(
        String achievementType,
        Long achievementId,
        EducationAchievementStatus currentStatus,
        EducationAchievementStatus nextStatus,
        String actionType,
        String reasonCode,
        String opinion,
        Long processedBy,
        LocalDateTime processedAt) {
}

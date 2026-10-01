package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDateTime;

/**
 * Describes one validated education-achievement state transition for the later transactional
 * persistence layer to append to education_achievement_status_histories.
 */
public record EducationAchievementStatusTransition(
        String previousStatus,
        String nextStatus,
        String actionType,
        String reasonCode,
        String opinion,
        Long processedBy,
        LocalDateTime processedAt
) {
}

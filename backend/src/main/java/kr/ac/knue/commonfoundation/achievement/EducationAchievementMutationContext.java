package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Carries the target identity and dates used by the common education-achievement mutation guard.
 */
public record EducationAchievementMutationContext(
        Long targetUserId,
        String evaluationYear,
        String organizationCode,
        LocalDate occurredDate,
        LocalDateTime requestedAt
) {
}

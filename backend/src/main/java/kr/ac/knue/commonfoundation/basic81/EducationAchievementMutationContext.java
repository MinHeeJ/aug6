package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;

/**
 * Describes the verified authorization and lifecycle context for an
 * education-achievement mutation before persistence begins.
 */
public record EducationAchievementMutationContext(
        Long targetUserId,
        String evaluationYear,
        LocalDate occurredDate) {
}

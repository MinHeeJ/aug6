package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/**
 * Carries the authoritative owner, organization, evaluation year, and occurrence date used by
 * education-achievement mutation guards before a persistence command is allowed to run.
 */
public record EducationAchievementCommandContext(
        Long targetUserId,
        String evaluationYear,
        String organizationCode,
        LocalDate occurredDate,
        String currentStatus
) {
}

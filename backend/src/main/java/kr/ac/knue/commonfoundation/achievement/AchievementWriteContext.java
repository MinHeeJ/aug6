package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/**
 * Supplies the ownership, evaluation, and occurred-date facts required before an education achievement is changed.
 */
public record AchievementWriteContext(
        Long targetUserId,
        String evaluationYear,
        String organizationCode,
        LocalDate occurredDate,
        String certificationStatus) {
}

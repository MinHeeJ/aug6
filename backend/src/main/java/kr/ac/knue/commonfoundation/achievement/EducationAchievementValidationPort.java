package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resolves persisted authorization, period, lock, and evaluation-date facts for education achievements.
 * The operation service owns the validation order; this port keeps database lookups behind the feature boundary.
 */
public interface EducationAchievementValidationPort {
    boolean hasDataScope(Long userId, Long ownerUserId, String organizationCode, String evaluationUnitCode);

    boolean hasActiveInputPeriod(String evaluationYear, String organizationCode, String evaluationUnitCode,
                                 LocalDateTime checkedAt);

    boolean hasEvaluationResultLock(String evaluationYear, String organizationCode, String evaluationUnitCode);

    boolean isWithinEvaluationDate(String evaluationYear, String organizationCode, LocalDate occurrenceDate);
}

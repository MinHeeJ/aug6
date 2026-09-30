package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import kr.ac.knue.commonfoundation.auth.CurrentUser;

/**
 * Immutable facts required to authorize and validate one education-achievement mutation.
 * Later vertical slices populate this context from their persisted achievement row and request payload.
 */
public record EducationAchievementValidationContext(
        CurrentUser user,
        Long ownerUserId,
        String evaluationYear,
        String organizationCode,
        String evaluationUnitCode,
        LocalDate occurrenceDate) {
}

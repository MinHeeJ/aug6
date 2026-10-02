package kr.ac.knue.commonfoundation.achievement;

/**
 * Describes non-blocking validation findings that the caller must return with a successful save.
 */
public record EducationAchievementGuardResult(boolean occurredDateOutOfRangeWarning) {
}

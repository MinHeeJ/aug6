package kr.ac.knue.commonfoundation.achievement;

/**
 * Returns non-blocking validation information that must be shown to the user after a permitted save.
 */
public record EducationAchievementValidationResult(boolean occurredDateWarning) {
}

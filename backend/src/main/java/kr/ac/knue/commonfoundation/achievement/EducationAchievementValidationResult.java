package kr.ac.knue.commonfoundation.achievement;

/**
 * Returns the non-blocking occurrence-date warning required by CMN-1102 after all blocking checks pass.
 */
public record EducationAchievementValidationResult(boolean occurrenceDateWarning) {
}

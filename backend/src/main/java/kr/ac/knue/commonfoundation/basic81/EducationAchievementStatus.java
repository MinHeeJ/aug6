package kr.ac.knue.commonfoundation.basic81;

/**
 * Stable persistence values for the education-achievement certification
 * lifecycle shared by all education achievement types.
 */
public enum EducationAchievementStatus {
    DRAFT,
    SUBMITTED,
    DEPARTMENT_CONFIRMED,
    DEPARTMENT_REJECTED,
    CERTIFIED,
    CERTIFICATION_REJECTED,
    EVALUATION_CONFIRMED,
    DELETED
}

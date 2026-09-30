package kr.ac.knue.commonfoundation.achievement;

/**
 * Persists an education-achievement status transition within the caller's transaction.
 */
public interface EducationAchievementStatusHistoryPort {
    void record(EducationAchievementStatusTransition transition);
}

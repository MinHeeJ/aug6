package kr.ac.knue.commonfoundation.educationachievement;

/** Returns the new status together with the append-only history row created by a transition. */
public record EducationAchievementTransitionResponse(String achievementStatus, EducationAchievementStatusHistory statusHistory) {
}

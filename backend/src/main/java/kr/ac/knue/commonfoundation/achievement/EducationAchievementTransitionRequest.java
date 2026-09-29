package kr.ac.knue.commonfoundation.achievement;

/**
 * Represents the common education-achievement transition input shared by the
 * four education achievement types.
 */
public record EducationAchievementTransitionRequest(
        String actionType,
        String reasonCode,
        String opinion) {
}

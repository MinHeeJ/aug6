package kr.ac.knue.commonfoundation.employmentrateachievements;

/** Save result including the non-blocking evaluation-period warning state. */
public record EmploymentRateAchievementSaveResponse(
        EmploymentRateAchievementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

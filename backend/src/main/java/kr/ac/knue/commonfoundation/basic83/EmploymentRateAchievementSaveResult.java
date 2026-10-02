package kr.ac.knue.commonfoundation.basic83;

/** Returns the saved 취업률 row with the non-blocking evaluation-date warning. */
public record EmploymentRateAchievementSaveResult(
        EmploymentRateAchievementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

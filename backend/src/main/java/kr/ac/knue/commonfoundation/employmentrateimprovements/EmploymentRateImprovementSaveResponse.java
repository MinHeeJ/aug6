package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Save result with the persisted row and the non-blocking evaluation-date warning. */
public record EmploymentRateImprovementSaveResponse(
        EmploymentRateImprovementAchievementResponse achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

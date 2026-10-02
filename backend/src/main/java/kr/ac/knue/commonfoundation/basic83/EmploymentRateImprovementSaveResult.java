package kr.ac.knue.commonfoundation.basic83;

/** Returns the saved 취업률 제고 row with the non-blocking evaluation-date warning. */
public record EmploymentRateImprovementSaveResult(
        EmploymentRateImprovementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

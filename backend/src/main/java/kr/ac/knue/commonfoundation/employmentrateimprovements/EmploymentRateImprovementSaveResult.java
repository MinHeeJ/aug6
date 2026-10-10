package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Saved aggregate and non-blocking evaluation-date warning. */
public record EmploymentRateImprovementSaveResult(
        EmploymentRateImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

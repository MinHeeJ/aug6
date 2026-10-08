package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Saved row and non-blocking evaluation-date warning. */
public record EmploymentRateImprovementSaveResult(
        EmploymentRateImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

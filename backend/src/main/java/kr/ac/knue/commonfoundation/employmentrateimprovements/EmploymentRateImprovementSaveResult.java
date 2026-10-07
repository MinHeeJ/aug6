package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Saved row and the non-blocking evaluation-date warning returned together. */
public record EmploymentRateImprovementSaveResult(
        EmploymentRateImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

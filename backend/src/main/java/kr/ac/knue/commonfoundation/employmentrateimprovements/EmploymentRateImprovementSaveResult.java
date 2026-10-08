package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Returns persisted values and the non-blocking evaluation-date warning. */
public record EmploymentRateImprovementSaveResult(
        EmploymentRateImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

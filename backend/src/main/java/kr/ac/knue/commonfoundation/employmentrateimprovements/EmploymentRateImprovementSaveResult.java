package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Readback plus a non-blocking evaluation-date warning. */
public record EmploymentRateImprovementSaveResult(
        EmploymentRateImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Stored row plus the nonblocking evaluation-date warning. */
public record EmploymentRateImprovementSaveResult(
        EmploymentRateImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

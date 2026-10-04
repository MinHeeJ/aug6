package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Returns the persisted row and the non-blocking evaluation-period warning. */
public record EmploymentRateImprovementSaveResponse(
        EmploymentRateImprovementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

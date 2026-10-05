package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Saved achievement plus the non-blocking evaluation-period warning contract. */
public record EmploymentRateImprovementSaveResult(
        EmploymentRateImprovementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

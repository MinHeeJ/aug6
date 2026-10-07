package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Persisted readback plus the non-blocking evaluation-date warning from the shared guard. */
public record EmploymentRateImprovementSaveResult(
        EmploymentRateImprovementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

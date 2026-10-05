package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Returns the reread persisted achievement and its non-blocking date warning. */
public record EmploymentRateImprovementSaveResult(
        EmploymentRateImprovementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

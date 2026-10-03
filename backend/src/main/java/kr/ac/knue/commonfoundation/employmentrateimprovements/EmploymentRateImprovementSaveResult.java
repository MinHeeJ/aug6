package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Returns the stored row and a non-blocking warning for dates outside the evaluation range. */
public record EmploymentRateImprovementSaveResult(
        EmploymentRateImprovementRow achievement,
        boolean achievementDateWarning,
        String warningMessage) {
}

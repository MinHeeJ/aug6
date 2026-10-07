package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Normalized, bindable filter and pagination values; no computed MyBatis properties. */
public record EmploymentRateImprovementSearchCriteria(
        int page, int pageSize, long rowOffset, String managementNo,
        String managementItemCode, String achievementStatus, String evaluationYear) {
}

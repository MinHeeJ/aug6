package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Normalized filters and pagination passed to both list and count queries. */
public record EmploymentRateImprovementSearchCriteria(
        int page, int pageSize, long pageOffset, String managementItemCode, String achievementStatus) {
}

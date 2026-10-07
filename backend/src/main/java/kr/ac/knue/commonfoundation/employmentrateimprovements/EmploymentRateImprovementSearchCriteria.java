package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Precomputed pagination and optional predicates bind through actual record components. */
public record EmploymentRateImprovementSearchCriteria(
        int page, int pageSize, long rowOffset, String managementNo, String managementItemCode,
        String certificationStatus) {
}

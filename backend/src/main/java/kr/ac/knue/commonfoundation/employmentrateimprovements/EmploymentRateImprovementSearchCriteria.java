package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Normalized, bound query values; pagination arithmetic belongs to the service. */
public record EmploymentRateImprovementSearchCriteria(
        int page, int pageSize, long pageOffset,
        String managementNo, String teacherName, String managementItemCode, String certificationStatus) {
}

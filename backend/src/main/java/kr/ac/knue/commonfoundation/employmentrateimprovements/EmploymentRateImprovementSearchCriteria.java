package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Normalized pagination and optional filters shared by list and count SQL. */
public record EmploymentRateImprovementSearchCriteria(
        int page, int pageSize, long rowOffset,
        String managementNo, String teacherName, String managementItemCode, String certificationStatus) {
}

package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Validated query values; offset is computed before MyBatis property binding. */
public record EmploymentRateImprovementSearch(
        int page, int pageSize, int rowOffset, String managementNo,
        String teacherName, String managementItemCode, String achievementStatus) {
}

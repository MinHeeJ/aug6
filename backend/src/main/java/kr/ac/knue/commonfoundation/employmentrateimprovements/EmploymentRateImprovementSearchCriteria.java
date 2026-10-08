package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Normalized paging/filter values, all directly bindable by MyBatis. */
public record EmploymentRateImprovementSearchCriteria(
        int page, int pageSize, long pageOffset, String managementItemCode,
        String evaluationYear, String achievementStatus, String teacherName) {
}

package kr.ac.knue.commonfoundation.courseoperations;

/** Normalized query values, including computed pagination bound as actual record properties. */
public record CourseOperationSearchCriteria(
        int page, int pageSize, long offset,
        String managementNo, String teacherName, String managementItemCode, String achievementStatus) {
}

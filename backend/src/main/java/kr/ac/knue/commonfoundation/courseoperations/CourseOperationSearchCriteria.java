package kr.ac.knue.commonfoundation.courseoperations;

/** Normalized filters and concrete pagination values, never mapper helper-method bindings. */
public record CourseOperationSearchCriteria(
        int page,
        int pageSize,
        long offset,
        String managementNo,
        String teacherName,
        String managementItemCode,
        String achievementStatus) {
}

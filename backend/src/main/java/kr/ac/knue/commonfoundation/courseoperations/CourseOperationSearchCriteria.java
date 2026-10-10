package kr.ac.knue.commonfoundation.courseoperations;

/** Normalized, bindable pagination and optional filter properties. */
public record CourseOperationSearchCriteria(
        int page, int pageSize, long pageOffset,
        String managementNo, String teacherName, String managementItemCode, String achievementStatus) {
}

package kr.ac.knue.commonfoundation.courseoperations;

/** Normalized, bindable search values; pagination is computed before mapper invocation. */
public record CourseOperationSearchCriteria(
        int page, int pageSize, long offset,
        String managementItemCode, String evaluationYear, String achievementStatus, String teacherName) {
}

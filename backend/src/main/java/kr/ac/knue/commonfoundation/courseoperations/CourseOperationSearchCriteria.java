package kr.ac.knue.commonfoundation.courseoperations;

/** Normalized search inputs; pagination is computed by the service, not bound as helper methods. */
public record CourseOperationSearchCriteria(
        int page,
        int pageSize,
        String managementNo,
        String teacherName,
        String managementItemCode,
        String achievementStatus) {
}

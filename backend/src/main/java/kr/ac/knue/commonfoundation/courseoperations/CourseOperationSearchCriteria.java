package kr.ac.knue.commonfoundation.courseoperations;

/** Normalized filters and pagination; every bound value is an actual record property. */
public record CourseOperationSearchCriteria(
        int page, int pageSize, int offset,
        String managementNo, String teacherName, String managementItemCode, String achievementStatus) {
}

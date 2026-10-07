package kr.ac.knue.commonfoundation.courseoperations;

/** Normalized pagination and optional filters; bind only present predicates. */
public record CourseOperationSearchCriteria(
        int page, int pageSize, String managementNo, String managementItemCode, String achievementStatus) {
}

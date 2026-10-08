package kr.ac.knue.commonfoundation.lectureimprovements;

/** SQL-ready pagination and optional filters, normalized before parameter binding. */
public record LectureImprovementSearchCriteria(
        int page, int pageSize, long offset, String managementItemCode, String achievementStatus) {
}

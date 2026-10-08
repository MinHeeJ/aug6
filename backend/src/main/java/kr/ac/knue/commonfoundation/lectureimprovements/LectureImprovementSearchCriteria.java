package kr.ac.knue.commonfoundation.lectureimprovements;

/** Normalized paging and optional filters; every bound value is a real record component. */
public record LectureImprovementSearchCriteria(
        int page, int pageSize, long rowOffset, String managementItemCode, String academicYear, String semester) {
}

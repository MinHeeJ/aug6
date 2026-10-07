package kr.ac.knue.commonfoundation.lectureimprovements;

/** Normalized pagination and optional predicates shared by list and count. */
public record LectureImprovementSearchCriteria(
        int page, int pageSize, long offset, String managementNo,
        String teacherName, String managementItemCode, String achievementStatus) {
}

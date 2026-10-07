package kr.ac.knue.commonfoundation.lectureimprovements;

/** Normalized query values; pagination bindings are real record components rather than helper properties. */
public record LectureImprovementSearchCriteria(
        int page, int pageSize, long rowOffset,
        String managementNo, String teacherName, String managementItemCode, String achievementStatus,
        String academicYear, String semester) {
}

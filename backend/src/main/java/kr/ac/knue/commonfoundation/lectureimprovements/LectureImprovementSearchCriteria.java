package kr.ac.knue.commonfoundation.lectureimprovements;

/** Normalized, bound properties; pagination offset is computed before MyBatis binding. */
public record LectureImprovementSearchCriteria(
        int page, int pageSize, long rowOffset, String managementItemCode, String managementNo,
        String teacherName, String achievementStatus) {
}

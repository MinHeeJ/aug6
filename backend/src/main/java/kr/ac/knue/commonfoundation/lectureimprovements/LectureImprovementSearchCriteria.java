package kr.ac.knue.commonfoundation.lectureimprovements;

/** Normalized query properties are directly bindable by MyBatis. */
public record LectureImprovementSearchCriteria(
        int page, int pageSize, int rowOffset, String managementNo, String teacherName,
        String managementItemCode, String achievementStatus, String evaluationYear) {
}

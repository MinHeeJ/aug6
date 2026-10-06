package kr.ac.knue.commonfoundation.lectureimprovements;

/** Normalized filters and computed pagination; MyBatis binds real record components. */
public record LectureImprovementSearchCriteria(
        int page,
        int pageSize,
        long rowOffset,
        String managementNo,
        String teacherName,
        String managementItemCode,
        String achievementStatus) {
}

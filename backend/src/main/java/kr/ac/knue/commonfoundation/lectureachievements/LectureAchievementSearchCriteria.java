package kr.ac.knue.commonfoundation.lectureachievements;

/** Optional, normalized search predicates for lecture achievements. */
public record LectureAchievementSearchCriteria(String managementNo, String teacherName,
        String managementItemCode, String certificationStatus) {
}

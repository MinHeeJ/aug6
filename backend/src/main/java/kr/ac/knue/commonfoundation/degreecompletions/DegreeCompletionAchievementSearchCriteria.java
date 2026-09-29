package kr.ac.knue.commonfoundation.degreecompletions;

/** Optional, normalized search predicates for degree-completion achievements. */
public record DegreeCompletionAchievementSearchCriteria(String managementNo, String teacherName,
        String certificationStatus) {
}

package kr.ac.knue.commonfoundation.achievement;

/** Optional degree-completion search filters; absent values are excluded from SQL predicates. */
public record DegreeCompletionAchievementSearchCriteria(String evaluationYear, String managementNo, String teacherName,
        String managementItemCode, String certificationStatus, int limit, int offset) {
}

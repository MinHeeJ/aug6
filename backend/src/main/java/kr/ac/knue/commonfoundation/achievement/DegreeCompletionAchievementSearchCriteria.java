package kr.ac.knue.commonfoundation.achievement;

/** Search filters for degree-completion lists; mapper predicates are included only for supplied values. */
public record DegreeCompletionAchievementSearchCriteria(int page, int pageSize, String managementNo,
        String teacherName, EducationAchievementStatus certificationStatus) {
}

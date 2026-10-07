package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Normalized SQL-bound filters and pagination; every component is a real MyBatis property. */
public record EmploymentRateImprovementCriteria(
        int page,
        int pageSize,
        long rowOffset,
        String managementNo,
        String teacherName,
        String managementItemCode,
        String achievementStatus) {
}

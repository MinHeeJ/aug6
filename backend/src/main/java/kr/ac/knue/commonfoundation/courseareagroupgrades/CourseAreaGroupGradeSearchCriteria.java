package kr.ac.knue.commonfoundation.courseareagroupgrades;

/**
 * Holds validated optional filters for the read-only course-area group grade query.
 * Normalization keeps mapper predicates parameterized and prevents null-bound SQL predicates.
 */
public record CourseAreaGroupGradeSearchCriteria(
        int page,
        int pageSize,
        String completionTypeCode,
        String semesterCode,
        String courseAreaCode,
        Long facultyUserId) {
    public int offset() {
        return page * pageSize;
    }
}

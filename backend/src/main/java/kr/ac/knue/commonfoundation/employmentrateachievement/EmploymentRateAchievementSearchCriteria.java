package kr.ac.knue.commonfoundation.employmentrateachievement;

/** Captures the only supported pagination values for employment-rate achievement reads. */
public record EmploymentRateAchievementSearchCriteria(int page, int pageSize) {
    public int offset() {
        return page * pageSize;
    }
}

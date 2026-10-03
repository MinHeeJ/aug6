package kr.ac.knue.commonfoundation.employmentrateachievement;

import java.util.List;

/** Paged response returned by the employment-rate achievement list operation. */
public record EmploymentRateAchievementSearchResponse(
        List<EmploymentRateAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

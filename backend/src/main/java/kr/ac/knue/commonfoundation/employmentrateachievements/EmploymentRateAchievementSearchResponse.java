package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;

/** Paged employment-rate achievement response that retains the requested pagination metadata. */
public record EmploymentRateAchievementSearchResponse(
        List<EmploymentRateAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

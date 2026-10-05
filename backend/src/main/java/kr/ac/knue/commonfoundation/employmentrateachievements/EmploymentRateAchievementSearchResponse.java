package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;

/** Paginated caller-scoped employment-rate achievement results. */
public record EmploymentRateAchievementSearchResponse(
        List<EmploymentRateAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

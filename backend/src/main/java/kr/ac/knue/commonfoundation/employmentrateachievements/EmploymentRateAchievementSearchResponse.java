package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;

/** Paginated employment-rate achievement response used by the list screen. */
public record EmploymentRateAchievementSearchResponse(
        List<EmploymentRateAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

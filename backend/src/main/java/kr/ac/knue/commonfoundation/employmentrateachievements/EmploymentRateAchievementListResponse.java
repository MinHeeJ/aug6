package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;

/** Page envelope for caller-scoped employment-rate achievement rows. */
public record EmploymentRateAchievementListResponse(
        List<EmploymentRateAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Paginated caller-scoped list response for 취업률 제고 실적 management. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementAchievementResponse> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

package kr.ac.knue.commonfoundation.basic83;

import java.util.List;

/** Paged response for the 20, 50, and 100-row 취업률 list. */
public record EmploymentRateAchievementSearchResponse(
        List<EmploymentRateAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

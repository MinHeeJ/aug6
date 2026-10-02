package kr.ac.knue.commonfoundation.basic83;

import java.util.List;

/** Paged response for the 20, 50, and 100-row 취업률 제고 list. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

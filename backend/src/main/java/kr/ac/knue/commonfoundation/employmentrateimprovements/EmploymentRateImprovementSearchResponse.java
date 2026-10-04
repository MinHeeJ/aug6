package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Paged response for the approved 20, 50, and 100-row 취업률 제고 view. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

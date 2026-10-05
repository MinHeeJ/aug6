package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Provides a paged, caller-scoped 취업률 제고 실적 list response. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

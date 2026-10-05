package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Paginated result for caller-scoped 취업률 제고 achievement searches. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Scoped list and count use exactly the same predicates. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements, int page, int pageSize, long totalElements) {
}

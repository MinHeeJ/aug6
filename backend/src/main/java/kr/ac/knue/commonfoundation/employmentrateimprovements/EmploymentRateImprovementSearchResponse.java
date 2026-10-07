package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Paged rows and a total using the same authorization and filter predicates. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

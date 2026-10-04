package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Paged employment-rate improvement list returned by the list operation. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

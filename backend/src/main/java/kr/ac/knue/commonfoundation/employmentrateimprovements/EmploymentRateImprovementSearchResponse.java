package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Scoped rows and the count from the identical SQL predicate. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements,
        List<ManagementItemOption> managementItems) {
}

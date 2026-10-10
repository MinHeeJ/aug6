package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Scoped, paginated results and input options sourced from the education management-item registry. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements,
        int page, int pageSize, long totalElements,
        List<String> managementItemCodes, boolean canCreate, boolean canUpdate) {
}

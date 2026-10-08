package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Scoped rows with total and DB-backed selectable teacher management items. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements,
        List<String> managementItems) {
}

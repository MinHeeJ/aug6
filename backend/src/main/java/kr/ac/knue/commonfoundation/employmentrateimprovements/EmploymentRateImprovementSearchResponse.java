package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Scoped page and database-backed input options. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements, int page, int pageSize, long totalElements,
        List<EmploymentRateImprovementManagementItem> managementItems) {
}

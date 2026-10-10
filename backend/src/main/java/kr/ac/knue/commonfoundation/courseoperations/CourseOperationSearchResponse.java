package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** Caller-scoped rows, matching count, and active database-backed management options. */
public record CourseOperationSearchResponse(
        List<CourseOperationRow> achievements,
        int page, int pageSize, long totalElements,
        List<CourseOperationManagementItem> managementItems) {
}

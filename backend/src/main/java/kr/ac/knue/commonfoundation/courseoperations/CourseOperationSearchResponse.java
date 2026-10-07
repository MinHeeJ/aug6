package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** Scoped page and DB-backed management item choices. */
public record CourseOperationSearchResponse(
        List<CourseOperationRow> achievements, int page, int pageSize, long totalElements,
        List<String> managementItems) {
}

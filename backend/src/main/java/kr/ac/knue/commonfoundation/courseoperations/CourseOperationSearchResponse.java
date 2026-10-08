package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** Scoped page and input settings returned by the list operation. */
public record CourseOperationSearchResponse(
        List<CourseOperationRow> achievements, int page, int pageSize, long totalElements,
        List<CourseOperationItemOption> managementItems) {
}

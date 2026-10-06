package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** Scoped list and matching total for the same filters. */
public record CourseOperationSearchResponse(
        List<CourseOperationRow> achievements, int page, int pageSize, long totalElements) {
}

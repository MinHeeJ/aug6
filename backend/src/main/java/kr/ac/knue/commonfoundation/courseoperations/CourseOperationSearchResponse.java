package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** Rows and count share the same scope and optional predicates. */
public record CourseOperationSearchResponse(
        List<CourseOperationRow> achievements, int page, int pageSize, long totalElements) {
}

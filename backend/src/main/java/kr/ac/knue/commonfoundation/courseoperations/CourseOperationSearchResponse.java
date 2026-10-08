package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** A scoped page and the count produced by exactly the same predicates. */
public record CourseOperationSearchResponse(
        List<CourseOperationRow> achievements, int page, int pageSize, long totalElements) {
}

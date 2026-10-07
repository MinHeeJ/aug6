package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** Preserves the established education-achievement pagination envelope. */
public record CourseOperationSearchResponse(
        List<CourseOperationRow> achievements, int page, int pageSize, long totalElements) {
}

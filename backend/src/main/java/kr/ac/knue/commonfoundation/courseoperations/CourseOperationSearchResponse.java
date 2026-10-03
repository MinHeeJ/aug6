package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** Paged API response for the course offering and operation list. */
public record CourseOperationSearchResponse(
        List<CourseOperationRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

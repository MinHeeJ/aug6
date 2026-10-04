package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** Paged response for the course-operation list screen's supported display sizes. */
public record CourseOperationSearchResponse(
        List<CourseOperationRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** Paginated list envelope returned by the course-operation list operation. */
public record CourseOperationSearchResponse(
        List<CourseOperationRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

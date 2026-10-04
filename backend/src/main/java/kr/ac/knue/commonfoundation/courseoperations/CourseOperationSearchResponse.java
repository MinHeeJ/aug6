package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** Paged list response for caller-scoped course-operation achievements. */
public record CourseOperationSearchResponse(
        List<CourseOperationResponse> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

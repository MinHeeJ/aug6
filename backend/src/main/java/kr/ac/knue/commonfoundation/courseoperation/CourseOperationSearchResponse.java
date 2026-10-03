package kr.ac.knue.commonfoundation.courseoperation;

import java.util.List;

/** Paged response for the approved course-operation list page sizes. */
public record CourseOperationSearchResponse(
        List<CourseOperationResponse> courseOperations,
        int page,
        int pageSize,
        long totalElements) {
}

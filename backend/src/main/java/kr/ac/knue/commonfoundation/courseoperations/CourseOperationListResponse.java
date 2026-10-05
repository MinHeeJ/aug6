package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** Paginated caller-scoped course-operation achievement result. */
public record CourseOperationListResponse(
        List<CourseOperationRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

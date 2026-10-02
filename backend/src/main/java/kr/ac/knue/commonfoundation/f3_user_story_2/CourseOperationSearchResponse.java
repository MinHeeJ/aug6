package kr.ac.knue.commonfoundation.f3_user_story_2;

import java.util.List;

/** Paged result for the course-operation list with the approved page-size choices. */
public record CourseOperationSearchResponse(
        List<CourseOperationResponse> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

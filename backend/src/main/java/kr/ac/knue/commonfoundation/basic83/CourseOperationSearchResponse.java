package kr.ac.knue.commonfoundation.basic83;

import java.util.List;

/** Represents the paged course-operation list returned by the approved API. */
public record CourseOperationSearchResponse(
        List<CourseOperationAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

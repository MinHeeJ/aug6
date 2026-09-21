package kr.ac.knue.commonfoundation.courseareagroupgrades;

import java.util.List;

/**
 * Paginated read-only response for the course-area group grade list.
 */
public record CourseAreaGroupGradeSearchResponse(
        List<CourseAreaGroupGradeItem> items,
        int page,
        int pageSize,
        long totalElements) {}

package com.example.faculty.achievement;

import java.util.List;

/**
 * Carries a paged lecture-evaluation result while preserving the requested page-size contract.
 */
public record LectureEvaluationAchievementSearchResponse(
        List<LectureEvaluationAchievementRow> rows,
        int page,
        int pageSize,
        long totalElements
) {
}

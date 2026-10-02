package kr.ac.knue.commonfoundation.basic81;

import java.util.List;

/**
 * Paged lecture-evaluation list response that preserves the API pagination
 * contract for the 20, 50, and 100 row display choices.
 */
public record LectureEvaluationAchievementSearchResponse(
        List<LectureEvaluationAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

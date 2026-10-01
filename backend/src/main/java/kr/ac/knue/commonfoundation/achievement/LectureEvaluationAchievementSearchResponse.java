package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/**
 * Stable paged response envelope payload for lecture-evaluation achievement searches.
 */
public record LectureEvaluationAchievementSearchResponse(
        List<LectureEvaluationAchievementRow> achievements,
        int page,
        int size,
        long totalElements
) {
}

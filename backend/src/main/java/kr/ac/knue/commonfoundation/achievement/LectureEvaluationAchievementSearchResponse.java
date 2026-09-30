package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/**
 * Returns the requested page of lecture-evaluation achievements and its total row count.
 */
public record LectureEvaluationAchievementSearchResponse(
        List<LectureEvaluationAchievementRow> achievements,
        int page,
        int size,
        long totalElements) {
}

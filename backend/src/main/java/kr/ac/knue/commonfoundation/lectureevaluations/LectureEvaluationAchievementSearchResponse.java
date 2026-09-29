package kr.ac.knue.commonfoundation.lectureevaluations;

import java.util.List;

/** Paged result for the lecture-evaluation management screen. */
public record LectureEvaluationAchievementSearchResponse(List<LectureEvaluationAchievementRow> achievements,
        int page, int pageSize, long totalElements) {
}

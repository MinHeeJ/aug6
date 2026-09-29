package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/** Paginated response envelope payload for the lecture-evaluation list operation. */
public record LectureEvaluationAchievementSearchResponse(
        List<LectureEvaluationAchievementRow> lectureEvaluationAchievements,
        int page,
        int pageSize,
        long totalElements) {
}

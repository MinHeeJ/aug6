package kr.ac.knue.commonfoundation.faculty.achievement;

import java.util.List;

/** Paginated lecture-evaluation list response used by the education achievement screen. */
public record LectureEvaluationAchievementSearchResponse(
        List<LectureEvaluationAchievementRow> achievements,
        int page,
        int size,
        long totalElements
) {
}

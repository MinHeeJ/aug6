package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/** Stable paged response payload for lecture achievement searches. */
public record LectureAchievementSearchResponse(
        List<LectureAchievementRow> achievements,
        int page,
        int size,
        long totalElements
) {
}

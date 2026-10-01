package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/** Stable paged response envelope for degree-completion achievement searches. */
public record DegreeCompletionAchievementSearchResponse(
        List<DegreeCompletionAchievementRow> achievements,
        int page,
        int size,
        long totalElements
) {
}

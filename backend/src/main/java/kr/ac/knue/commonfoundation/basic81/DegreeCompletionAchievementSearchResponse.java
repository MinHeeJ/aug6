package kr.ac.knue.commonfoundation.basic81;

import java.util.List;

/** Paged degree-completion list response using the approved 20, 50, and 100 row sizes. */
public record DegreeCompletionAchievementSearchResponse(
        List<DegreeCompletionAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

package kr.ac.knue.commonfoundation.degreecompletions;

import java.util.List;

/** Paged result for the degree-completion achievement management screen. */
public record DegreeCompletionAchievementSearchResponse(List<DegreeCompletionAchievementRow> achievements,
        int page, int pageSize, long totalElements) {
}

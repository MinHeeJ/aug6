package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/** Paginated payload for the degree-completion list contract. */
public record DegreeCompletionAchievementSearchResponse(List<DegreeCompletionAchievementRow> degreeCompletionAchievements,
        int page, int pageSize, long totalElements) {
}

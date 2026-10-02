package kr.ac.knue.commonfoundation.basic83;

import java.util.List;

/** Represents the paged teaching-improvement list returned by the approved API. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

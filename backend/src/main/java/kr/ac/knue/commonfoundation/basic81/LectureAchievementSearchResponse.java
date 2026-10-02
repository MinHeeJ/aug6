package kr.ac.knue.commonfoundation.basic81;

import java.util.List;

/** Paged list response for the approved 20, 50, and 100-row lecture view. */
public record LectureAchievementSearchResponse(
        List<LectureAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

package kr.ac.knue.commonfoundation.lectureachievements;

import java.util.List;

/** Paged result for the lecture-achievement management screen. */
public record LectureAchievementSearchResponse(List<LectureAchievementRow> achievements,
        int page, int pageSize, long totalElements) {
}

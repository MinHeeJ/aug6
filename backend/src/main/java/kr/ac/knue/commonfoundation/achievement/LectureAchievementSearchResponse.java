package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/** Paginated response envelope payload for the lecture list operation. */
public record LectureAchievementSearchResponse(
        List<LectureAchievementRow> lectureAchievements,
        int page,
        int pageSize,
        long totalElements) {
}

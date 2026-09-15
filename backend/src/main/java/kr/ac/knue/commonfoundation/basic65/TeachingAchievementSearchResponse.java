package kr.ac.knue.commonfoundation.basic65;

import java.util.List;

public record TeachingAchievementSearchResponse(
        List<TeachingAchievementRow> teachingAchievements, int page, int size, long totalElements) {
}

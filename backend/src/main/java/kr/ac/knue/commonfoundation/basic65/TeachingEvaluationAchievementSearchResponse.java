package kr.ac.knue.commonfoundation.basic65;

import java.util.List;

public record TeachingEvaluationAchievementSearchResponse(
        List<TeachingEvaluationAchievementRow> teachingEvaluationAchievements,
        int page,
        int size,
        long totalElements) {
}

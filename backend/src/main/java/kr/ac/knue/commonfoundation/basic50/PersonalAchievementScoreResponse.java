package kr.ac.knue.commonfoundation.basic50;

import java.math.BigDecimal;
import java.util.List;

public record PersonalAchievementScoreResponse(
        Long teacherUserId,
        String teacherName,
        String evaluationYear,
        BigDecimal totalScore,
        List<PersonalScoreSummary> summaries,
        List<PersonalScoreItem> items) {
}

package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/** Save result including non-blocking occurrence-date warnings. */
public record LectureEvaluationAchievementSaveResponse(
        LectureEvaluationAchievementRow lectureEvaluationAchievement,
        List<String> warnings) {
}

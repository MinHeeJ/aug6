package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/** Save result including non-blocking occurrence-date warnings. */
public record LectureAchievementSaveResponse(
        LectureAchievementRow lectureEvaluationAchievement,
        List<String> warnings) {
}

package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/** Save result including non-blocking occurrence-date warnings. */
public record DegreeCompletionAchievementSaveResponse(DegreeCompletionAchievementRow degreeCompletionAchievement,
        List<String> warnings) {
}

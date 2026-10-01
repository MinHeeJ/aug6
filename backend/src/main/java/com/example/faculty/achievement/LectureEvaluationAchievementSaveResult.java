package com.example.faculty.achievement;

/**
 * Returns the persisted achievement together with the non-blocking evaluation-period warning state.
 */
public record LectureEvaluationAchievementSaveResult(
        LectureEvaluationAchievementRow achievement,
        boolean warning,
        String warningCode
) {
}

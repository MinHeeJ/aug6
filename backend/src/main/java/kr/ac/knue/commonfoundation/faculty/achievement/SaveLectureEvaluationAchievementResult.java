package kr.ac.knue.commonfoundation.faculty.achievement;

/** Save result preserves the record and explicitly communicates an allowed period-boundary warning. */
public record SaveLectureEvaluationAchievementResult(
        LectureEvaluationAchievementRow achievement,
        boolean occurredDateWarning
) {
}

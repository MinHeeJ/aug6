package kr.ac.knue.commonfoundation.basic81;

/**
 * Returns the saved lecture-evaluation row with the non-blocking
 * occurred-date warning required by the education-achievement contract.
 */
public record LectureEvaluationAchievementSaveResult(
        LectureEvaluationAchievementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

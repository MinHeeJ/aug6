package kr.ac.knue.commonfoundation.basic83;

/** Returns a persisted teaching-improvement row and any permitted occurred-date warning. */
public record LectureImprovementSaveResult(
        LectureImprovementAchievementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

package kr.ac.knue.commonfoundation.basic81;

/** Returns the persisted lecture row and a non-blocking occurred-date warning. */
public record LectureAchievementSaveResult(
        LectureAchievementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

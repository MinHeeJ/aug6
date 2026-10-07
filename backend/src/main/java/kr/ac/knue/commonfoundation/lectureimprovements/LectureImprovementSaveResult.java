package kr.ac.knue.commonfoundation.lectureimprovements;

/** Re-readable saved identity with the nonblocking date warning and correlated audit request. */
public record LectureImprovementSaveResult(
        Long achievementId, LectureImprovementRow achievement,
        boolean occurredDateWarning, String warningMessage, String requestId) {
}

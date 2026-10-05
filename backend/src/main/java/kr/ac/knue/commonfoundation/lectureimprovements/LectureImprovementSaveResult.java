package kr.ac.knue.commonfoundation.lectureimprovements;

/** Returns the stored achievement together with the non-blocking period warning. */
public record LectureImprovementSaveResult(
        LectureImprovementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

package kr.ac.knue.commonfoundation.lectureimprovements;

/** Save response preserves the non-blocking occurrence-period warning convention. */
public record LectureImprovementSaveResult(
        LectureImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

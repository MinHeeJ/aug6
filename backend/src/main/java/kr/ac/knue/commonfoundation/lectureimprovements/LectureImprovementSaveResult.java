package kr.ac.knue.commonfoundation.lectureimprovements;

/** Save readback plus the non-blocking evaluation-date warning. */
public record LectureImprovementSaveResult(
        LectureImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

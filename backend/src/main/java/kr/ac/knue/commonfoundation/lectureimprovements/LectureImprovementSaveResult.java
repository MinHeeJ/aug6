package kr.ac.knue.commonfoundation.lectureimprovements;

/** A persisted source and the non-blocking evaluation-date warning. */
public record LectureImprovementSaveResult(
        LectureImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

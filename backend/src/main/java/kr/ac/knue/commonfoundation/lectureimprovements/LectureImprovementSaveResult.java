package kr.ac.knue.commonfoundation.lectureimprovements;

/** Saved values and the nonblocking evaluation-date warning. */
public record LectureImprovementSaveResult(
        LectureImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

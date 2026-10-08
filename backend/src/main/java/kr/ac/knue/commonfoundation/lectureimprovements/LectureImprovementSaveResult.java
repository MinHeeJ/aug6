package kr.ac.knue.commonfoundation.lectureimprovements;

/** Returns persisted values with a nonblocking evaluation-date warning. */
public record LectureImprovementSaveResult(
        LectureImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

package kr.ac.knue.commonfoundation.lectureimprovements;

/** Returns the committed representation with the existing nonblocking date warning. */
public record LectureImprovementSaveResult(
        LectureImprovementRow achievement, boolean occurredDateWarning, String warningMessage) {
}

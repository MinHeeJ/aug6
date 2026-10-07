package kr.ac.knue.commonfoundation.courseoperations;

/** Saved row with the non-blocking evaluation-date warning from the shared guard. */
public record CourseOperationSaveResult(
        CourseOperationRow achievement, boolean occurredDateWarning, String warningMessage) {
}

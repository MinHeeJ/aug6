package kr.ac.knue.commonfoundation.courseoperations;

/** Saved row and non-blocking evaluation-date warning. */
public record CourseOperationSaveResult(
        CourseOperationRow achievement, boolean occurredDateWarning, String warningMessage) {
}

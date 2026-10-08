package kr.ac.knue.commonfoundation.courseoperations;

/** Saved database row and nonblocking evaluation-date warning. */
public record CourseOperationSaveResult(
        CourseOperationRow achievement, boolean occurredDateWarning, String warningMessage) {
}

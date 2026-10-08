package kr.ac.knue.commonfoundation.courseoperations;

/** Saved values plus the nonblocking evaluation-date warning. */
public record CourseOperationSaveResult(
        CourseOperationRow achievement, boolean occurredDateWarning, String warningMessage) {
}

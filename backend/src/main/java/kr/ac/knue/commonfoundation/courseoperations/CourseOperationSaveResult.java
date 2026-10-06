package kr.ac.knue.commonfoundation.courseoperations;

/** Persisted row plus the non-blocking evaluation-period date warning. */
public record CourseOperationSaveResult(
        CourseOperationRow achievement, boolean occurredDateWarning, String warningMessage) {
}

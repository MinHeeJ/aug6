package kr.ac.knue.commonfoundation.courseoperations;

/** Includes the non-blocking occurrence-date warning after a successful atomic save. */
public record CourseOperationSaveResult(
        CourseOperationRow achievement, boolean occurredDateWarning, String warningMessage) {
}

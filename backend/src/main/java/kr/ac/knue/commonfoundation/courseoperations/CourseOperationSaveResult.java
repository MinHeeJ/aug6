package kr.ac.knue.commonfoundation.courseoperations;

/** Persisted detail plus the deliberately nonblocking achievement-date warning. */
public record CourseOperationSaveResult(
        CourseOperationRow achievement, boolean occurredDateWarning, String warningMessage) {
}

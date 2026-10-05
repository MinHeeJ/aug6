package kr.ac.knue.commonfoundation.courseoperations;

/** Save result including the non-blocking evaluation-period warning when applicable. */
public record CourseOperationSaveResponse(
        CourseOperationRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

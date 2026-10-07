package kr.ac.knue.commonfoundation.courseoperations;

/** D12 save result supports detail readback and a non-blocking evaluation-date warning. */
public record CourseOperationSaveResult(
        Long achievementId, CourseOperationRow achievement,
        boolean occurredDateWarning, String warningMessage, String requestId) {
}

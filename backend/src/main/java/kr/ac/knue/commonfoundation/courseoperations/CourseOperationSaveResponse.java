package kr.ac.knue.commonfoundation.courseoperations;

/** Save response keeps the period warning separate because an out-of-period date is not blocking. */
public record CourseOperationSaveResponse(
        CourseOperationResponse achievement,
        boolean achievementDateWarning,
        String warningMessage) {
}

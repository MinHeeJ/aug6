package kr.ac.knue.commonfoundation.courseoperations;

/** Save response including the required non-blocking achievement-date warning. */
public record CourseOperationSaveResponse(
        CourseOperationRow achievement,
        boolean achievementDateWarning,
        String warningMessage) {
}

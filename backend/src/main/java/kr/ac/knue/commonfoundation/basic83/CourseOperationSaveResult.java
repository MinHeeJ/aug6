package kr.ac.knue.commonfoundation.basic83;

/** Returns a persisted course-operation row and any permitted occurred-date warning. */
public record CourseOperationSaveResult(
        CourseOperationAchievementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

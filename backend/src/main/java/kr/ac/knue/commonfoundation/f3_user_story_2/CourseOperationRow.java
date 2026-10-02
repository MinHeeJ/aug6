package kr.ac.knue.commonfoundation.f3_user_story_2;

import java.time.LocalDate;

/** Persistence projection for the course-operation table and its owning user. */
public record CourseOperationRow(
        Long achievementId,
        String managementNo,
        Long targetUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate occurredDate,
        String performanceDetail,
        String certificationStatus,
        String attachmentIds) {
}

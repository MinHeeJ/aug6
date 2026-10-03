package kr.ac.knue.commonfoundation.courseoperation;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Persistence read model for a course offering and operation achievement. */
public record CourseOperationRow(
        Long achievementId,
        String managementNo,
        Long targetUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String performanceDetails,
        String attachmentIdsJson,
        String achievementStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

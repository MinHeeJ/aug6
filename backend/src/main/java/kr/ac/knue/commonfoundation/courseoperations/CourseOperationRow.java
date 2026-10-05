package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for a caller-visible course-operation achievement. */
public record CourseOperationRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String performanceDetails,
        String achievementStatus,
        String attachmentIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

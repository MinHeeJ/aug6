package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for one persisted course-operation achievement and its detail text. */
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
        String attachmentRef,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

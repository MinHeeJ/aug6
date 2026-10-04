package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model joining a course-operation header to its persisted performance detail. */
public record CourseOperationRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String performanceDetails,
        String attachmentIds,
        String achievementStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

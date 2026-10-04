package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Internal persistence projection for a course-operation achievement and its detail row. */
public record CourseOperationRow(
        Long achievementId,
        Long teacherUserId,
        String teacherName,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String performanceDetails,
        String attachmentIdsJson,
        String achievementStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

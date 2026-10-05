package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Joined education-achievement source and course-operation detail row returned by the API. */
public record CourseOperationRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String performanceDetails,
        String achievementStatus,
        String attachmentRefs,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

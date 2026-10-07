package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Joined FR-030 header and detail with immutable ownership and evaluation-year metadata. */
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
        String attachmentRef,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

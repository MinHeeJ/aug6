package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Materialized header and typed detail, including immutable ownership and evaluation year. */
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
        List<String> attachmentIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

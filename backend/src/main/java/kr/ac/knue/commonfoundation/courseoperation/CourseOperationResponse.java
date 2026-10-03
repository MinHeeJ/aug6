package kr.ac.knue.commonfoundation.courseoperation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Public course-operation representation that exposes opaque attachment identifiers only. */
public record CourseOperationResponse(
        Long achievementId,
        String managementNo,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String performanceDetails,
        List<String> attachmentIds,
        String achievementStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

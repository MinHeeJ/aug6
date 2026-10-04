package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** API representation of a course-operation achievement and its editable detail fields. */
public record CourseOperationResponse(
        Long achievementId,
        String teacherName,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String performanceDetails,
        List<String> attachmentIds,
        String achievementStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

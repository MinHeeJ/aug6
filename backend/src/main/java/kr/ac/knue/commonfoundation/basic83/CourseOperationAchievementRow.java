package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for a visible course offering and operation achievement. */
public record CourseOperationAchievementRow(
        Long achievementId,
        String managementNo,
        Long targetUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String performanceDetails,
        String attachmentRef,
        String certificationStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

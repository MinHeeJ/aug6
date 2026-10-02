package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for a visible employment-rate achievement and its lifecycle metadata. */
public record EmploymentRateAchievementRow(
        Long achievementId,
        String managementNo,
        Long targetUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String attachmentRef,
        String certificationStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

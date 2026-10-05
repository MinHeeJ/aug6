package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model returned for an employment-rate achievement header. */
public record EmploymentRateAchievementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String attachmentIds,
        String achievementStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

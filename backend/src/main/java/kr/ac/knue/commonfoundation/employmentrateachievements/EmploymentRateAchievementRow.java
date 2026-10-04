package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.time.LocalDateTime;
/** Read model for the common education-achievement header used by employment-rate records. */
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

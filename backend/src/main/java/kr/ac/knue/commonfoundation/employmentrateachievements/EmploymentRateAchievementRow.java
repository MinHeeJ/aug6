package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model returned for an employment-rate achievement list and detail view. */
public record EmploymentRateAchievementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String achievementStatus,
        String attachmentRefs,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read model returned by the employment-rate achievement list and detail operations. */
public record EmploymentRateAchievementRow(
        Long achievementId,
        String managementNo,
        Long targetUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String certificationStatus,
        List<String> attachmentIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

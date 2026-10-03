package kr.ac.knue.commonfoundation.employmentrateachievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read model for a visible employment-rate achievement. */
public record EmploymentRateAchievementRow(
        Long achievementId,
        Long targetUserId,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        List<String> attachmentIds,
        String certificationStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

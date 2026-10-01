package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Internal mapper projection for a degree-completion header before its details are materialized. */
public record DegreeCompletionAchievementHeaderRow(
        Long achievementId,
        String managementNo,
        Long targetUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate occurredDate,
        String achievementDetail,
        String certificationStatus,
        String attachmentRef,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

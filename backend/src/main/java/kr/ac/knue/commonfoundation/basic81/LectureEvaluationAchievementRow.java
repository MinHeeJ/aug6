package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read model for one visible lecture-evaluation achievement and its audit-safe
 * attachment reference; physical storage locations are never exposed.
 */
public record LectureEvaluationAchievementRow(
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

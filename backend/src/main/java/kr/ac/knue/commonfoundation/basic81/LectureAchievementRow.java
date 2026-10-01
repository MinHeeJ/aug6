package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for a visible lecture achievement without exposing file storage paths. */
public record LectureAchievementRow(
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

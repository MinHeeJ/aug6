package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read model for one lecture-evaluation achievement list or detail row without exposing physical
 * attachment storage metadata.
 */
public record LectureEvaluationAchievementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String evaluationYear,
        String organizationCode,
        String managementItemCode,
        LocalDate occurredDate,
        String achievementDetail,
        String certificationStatus,
        boolean attachmentPresent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

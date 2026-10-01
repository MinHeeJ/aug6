package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for a lecture-evaluation source record without exposing attachment storage paths. */
public record LectureEvaluationAchievementRow(
        Long achievementId,
        String managementNo,
        Long targetUserId,
        String teacherName,
        String managementItemCode,
        LocalDate occurredDate,
        String achievementDetail,
        String certificationStatus,
        boolean hasAttachment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

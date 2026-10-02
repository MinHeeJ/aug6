package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for a visible teaching-improvement achievement. */
public record LectureImprovementAchievementRow(
        Long achievementId,
        String managementNo,
        Long targetUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementContent,
        Integer academicYear,
        Integer semester,
        String attachmentRef,
        String certificationStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

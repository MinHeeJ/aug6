package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/**
 * Read model for a lecture-evaluation achievement list row. It deliberately
 * exposes attachment presence rather than the protected storage reference.
 */
public record LectureEvaluationAchievementRow(
        Long achievementId,
        String managementNo,
        String evaluationYear,
        Long teacherUserId,
        String teacherName,
        String managementItemCode,
        LocalDate occurredDate,
        String certificationStatus,
        boolean hasAttachment) {
}

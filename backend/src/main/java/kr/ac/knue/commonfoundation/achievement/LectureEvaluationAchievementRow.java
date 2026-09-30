package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/**
 * Presents a lecture-evaluation achievement in the list and selected-detail views.
 */
public record LectureEvaluationAchievementRow(
        Long achievementId,
        String managementNo,
        String teacherName,
        String managementItemCode,
        LocalDate occurredDate,
        String certificationStatus,
        boolean attachmentExists,
        String achievementDetail,
        String attachmentRef,
        String evaluationYear,
        Long targetUserId,
        String organizationCode) {
}

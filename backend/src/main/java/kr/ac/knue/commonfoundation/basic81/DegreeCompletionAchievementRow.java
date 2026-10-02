package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read model for a degree-completion achievement and its persisted student details. */
public record DegreeCompletionAchievementRow(
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
        List<DegreeCompletionStudent> students,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

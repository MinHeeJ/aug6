package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read model for a degree-completion achievement and its persisted student detail rows. */
public record DegreeCompletionAchievementRow(
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
        List<DegreeCompletionStudentRow> students,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

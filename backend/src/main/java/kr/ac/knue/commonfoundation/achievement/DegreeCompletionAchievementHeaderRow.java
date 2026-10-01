package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Internal persistence projection for a degree-completion header before its student rows are loaded. */
public record DegreeCompletionAchievementHeaderRow(
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

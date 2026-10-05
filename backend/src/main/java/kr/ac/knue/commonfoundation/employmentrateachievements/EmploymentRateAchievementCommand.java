package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;

/** Internal persistence command with actor and immutable ownership context. */
public record EmploymentRateAchievementCommand(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String attachmentIds,
        Long actorUserId) {
}

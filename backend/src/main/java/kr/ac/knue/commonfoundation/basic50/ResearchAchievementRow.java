package kr.ac.knue.commonfoundation.basic50;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResearchAchievementRow(
        Long achievementId,
        String evaluationYear,
        String organizationCode,
        Long teacherUserId,
        String teacherName,
        String title,
        String areaCode,
        String managementCriterionCode,
        String classificationCode,
        String confirmationStatus,
        LocalDate achievementDate,
        String sourceSystem,
        LocalDateTime confirmedAt,
        Long confirmedBy,
        LocalDateTime updatedAt) {
}

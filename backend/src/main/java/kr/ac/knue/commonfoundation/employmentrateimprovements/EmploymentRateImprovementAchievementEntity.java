package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Internal materialization of the BASIC-83 master row joined to its typed detail row. */
public record EmploymentRateImprovementAchievementEntity(
        Long achievementId,
        Long teacherUserId,
        String teacherLoginId,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String attachmentIdsJson,
        String achievementStatus,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        String mockExamQuestionPeriod,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for the common header and type-specific employment-rate improvement fields. */
public record EmploymentRateImprovementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherLoginId,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementStatus,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        String mockExamQuestionPeriod,
        String attachmentRef,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

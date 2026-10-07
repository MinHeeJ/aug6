package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Exact typed projection of the common header and improvement-detail join. */
public record EmploymentRateImprovementStoredRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
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

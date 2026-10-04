package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for a caller-scoped 취업률 제고 실적 without exposing storage paths. */
public record EmploymentRateImprovementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        String mockExamQuestionPeriod,
        String attachmentIds,
        String achievementStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

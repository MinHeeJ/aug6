package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;
/** Read model for a caller-visible 취업률 제고 실적 without attachment metadata. */
public record EmploymentRateImprovementRow(
        Long achievementId,
        String managementNo,
        Long targetUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        String mockExamQuestionPeriod,
        String certificationStatus,
        boolean hasAttachments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

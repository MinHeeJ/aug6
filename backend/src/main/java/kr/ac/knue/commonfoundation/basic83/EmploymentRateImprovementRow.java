package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for a visible 취업률 제고 achievement and its approved business fields. */
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
        String attachmentRef,
        String certificationStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

package kr.ac.knue.commonfoundation.employmentrateimprovement;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for one caller-visible 취업률 제고 실적 and its approved detail fields. */
public record EmploymentRateImprovementRow(
        Long achievementId,
        Long targetUserId,
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

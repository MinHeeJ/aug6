package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Joined normalized header and employment-improvement detail, including audit timestamps. */
public record EmploymentRateImprovementRow(
        Long achievementId,
        Long teacherUserId,
        String organizationCode,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String achievementDetail,
        String attachmentRef,
        String attachmentIds,
        String achievementStatus,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        String mockExamQuestionPeriod,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

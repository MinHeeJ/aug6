package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;

/** Materialized header and one-to-one employment improvement detail. */
public record EmploymentRateImprovementRow(
        Long achievementId,
        Long teacherUserId,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementStatus,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        String mockExamQuestionPeriod,
        String attachmentRef) {
}

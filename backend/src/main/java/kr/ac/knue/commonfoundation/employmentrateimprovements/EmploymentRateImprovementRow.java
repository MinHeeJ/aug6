package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;
/** Read model for a visible 취업률 제고 achievement and its one-to-one detail fields. */
public record EmploymentRateImprovementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String achievementStatus,
        String attachmentRefs,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        String mockExamQuestionPeriod,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

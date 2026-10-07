package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Complete persisted header and detail projection, also used for whole-row audit snapshots. */
public record EmploymentRateImprovementRow(
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
        List<String> attachmentIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

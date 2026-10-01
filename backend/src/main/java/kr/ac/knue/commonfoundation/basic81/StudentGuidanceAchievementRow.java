package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for a student-guidance header and its persisted student details. */
public record StudentGuidanceAchievementRow(
        Long achievementId,
        String managementNo,
        Long targetUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate guidanceStartDate,
        LocalDate guidanceEndDate,
        int studentCount,
        String certificationStatus,
        String attachmentRef,
        String studentsJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

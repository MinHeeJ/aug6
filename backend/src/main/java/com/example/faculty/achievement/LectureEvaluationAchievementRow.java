package com.example.faculty.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read model for a lecture-evaluation achievement returned through the education achievement API.
 */
public record LectureEvaluationAchievementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String evaluationYear,
        String organizationCode,
        String managementItemCode,
        LocalDate occurredDate,
        String achievementDetailJson,
        String certificationStatus,
        String attachmentRef,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

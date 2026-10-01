package com.example.faculty.achievement;

import java.time.LocalDate;

/**
 * Internal persistence command that serializes dynamic detail fields before binding them to PostgreSQL jsonb.
 */
public record LectureEvaluationAchievementCommand(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String evaluationYear,
        String organizationCode,
        String managementItemCode,
        LocalDate occurredDate,
        String achievementDetailJson,
        String attachmentRef,
        String certificationStatus,
        Long updatedBy
) {
}

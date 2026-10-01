package com.example.faculty.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Defines the validated write payload for creating or updating one lecture-evaluation achievement.
 */
public record SaveLectureEvaluationAchievementRequest(
        Long achievementId,
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "발생일을 입력하세요.") LocalDate occurredDate,
        JsonNode achievementDetail,
        String attachmentRef,
        String transitionAction,
        String reasonCode,
        String opinion
) {
}

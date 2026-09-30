package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Binds the supported editable fields for a lecture achievement save request.
 * The detail object is retained as JSON because the active FR-018 management-item configuration owns its shape.
 */
public record LectureAchievementRequest(
        Long achievementId,
        @NotBlank(message = "관리항목 코드를 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate occurredDate,
        JsonNode achievementDetail,
        EducationAchievementStatus nextStatus,
        String transitionReason,
        Integer attachmentCount,
        String changeReason) {
}

package kr.ac.knue.commonfoundation.basic81;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Defines the approved lecture-evaluation save payload. The detail is retained
 * as structured JSON because FR-018 determines its fields dynamically.
 */
public record SaveLectureEvaluationAchievementRequest(
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate occurredDate,
        JsonNode achievementDetail,
        String attachmentRef) {
}

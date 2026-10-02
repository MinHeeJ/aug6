package kr.ac.knue.commonfoundation.basic81;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Defines a lecture save command; achievementId selects an existing source row for an update. */
public record SaveLectureAchievementRequest(
        Long achievementId,
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate occurredDate,
        JsonNode achievementDetail,
        String attachmentRef) {
}

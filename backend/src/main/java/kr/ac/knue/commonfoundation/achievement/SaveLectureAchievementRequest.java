package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Boundary payload for creating or updating a lecture achievement. Optional target fields support
 * R02/R04 delegated entry while R01 defaults to the authenticated teacher.
 */
public record SaveLectureAchievementRequest(
        Long achievementId,
        Long targetUserId,
        String evaluationYear,
        String organizationCode,
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate occurredDate,
        JsonNode achievementDetail,
        String attachmentRef,
        String changeReason
) {
}

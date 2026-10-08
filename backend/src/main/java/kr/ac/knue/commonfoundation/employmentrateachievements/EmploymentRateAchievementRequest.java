package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Creation payload; update identity and evaluation year belong to the stored row. */
public record EmploymentRateAchievementRequest(
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @NotBlank @Size(max = 1000) String achievementName,
        JsonNode achievementDetail,
        @Size(max = 300) String attachmentRef) {
}

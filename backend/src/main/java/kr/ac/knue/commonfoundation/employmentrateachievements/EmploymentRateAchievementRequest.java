package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Individual input; the URL, never a body ID, selects the row to update. */
public record EmploymentRateAchievementRequest(
        Long teacherUserId,
        @NotBlank @Pattern(regexp = "[0-9]{4}") String evaluationYear,
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @Size(max = 300) String achievementName,
        JsonNode achievementDetail,
        @Size(max = 300) String attachmentRef) {
}

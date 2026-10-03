package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Command payload for creating or updating a faculty employment-rate achievement. */
public record EmploymentRateAchievementRequest(
        @NotBlank String managementItemCode,
        @NotNull LocalDate achievementDate,
        String achievementName,
        List<String> attachmentIds) {
}

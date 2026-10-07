package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Editable business fields; owner, evaluation year and status are server-controlled. */
public record EmploymentRateAchievementRequest(
        @NotBlank String managementItemCode,
        @NotNull LocalDate achievementDate,
        @Size(max = 500) String achievementName,
        List<String> attachmentIds) {
}

package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Individual employment achievement input; identity and owner are supplied by the server. */
public record EmploymentRateAchievementRequest(
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @Pattern(regexp = "[0-9]{4}") String evaluationYear,
        @NotBlank @Size(max = 500) String title,
        @Size(max = 300) String attachmentRef,
        String achievementStatus) {
}

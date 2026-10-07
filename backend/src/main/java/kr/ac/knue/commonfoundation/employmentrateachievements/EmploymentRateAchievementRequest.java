package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Individual command; ownership, evaluation year and lifecycle state are server controlled. */
public record EmploymentRateAchievementRequest(
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @Size(max = 500) String achievementName,
        @Size(max = 100000) String achievementDetail,
        @Size(max = 300) String attachmentRef) {
}

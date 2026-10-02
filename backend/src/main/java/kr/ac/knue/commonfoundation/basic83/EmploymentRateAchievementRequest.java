package kr.ac.knue.commonfoundation.basic83;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Defines the approved command for an individual employment-rate achievement. */
public record EmploymentRateAchievementRequest(
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        String achievementName,
        List<String> attachmentIds) {
}

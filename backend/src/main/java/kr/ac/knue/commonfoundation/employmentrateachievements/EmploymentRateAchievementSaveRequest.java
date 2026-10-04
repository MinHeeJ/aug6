package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Request body for creating or updating one employment-rate achievement. */
public record EmploymentRateAchievementSaveRequest(
        @NotBlank(message = "관리항목은 필수입니다.") String managementItemCode,
        @NotNull(message = "업적발생일은 필수입니다.") LocalDate achievementDate,
        String achievementName,
        List<String> attachmentIds) {
}

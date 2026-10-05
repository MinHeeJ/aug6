package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Request payload for creating or updating one employment-rate achievement. */
public record EmploymentRateAchievementRequest(
        @NotBlank(message = "관리항목코드를 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        String achievementName,
        List<String> attachmentIds) {
}

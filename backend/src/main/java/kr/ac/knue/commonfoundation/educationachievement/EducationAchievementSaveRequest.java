package kr.ac.knue.commonfoundation.educationachievement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * Carries common-master fields and type-specific details for an education achievement.
 */
public record EducationAchievementSaveRequest(
        Long achievementId,
        @NotBlank(message = "실적유형을 선택하세요.") String achievementType,
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "발생일을 입력하세요.") LocalDate occurrenceDate,
        List<@Valid DegreeCompletionDetail> degreeCompletionDetails,
        List<@Valid StudentGuidanceDetail> studentGuidanceDetails) {
}

package kr.ac.knue.commonfoundation.educationachievement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Defines the minimal common payload used to register an education achievement. */
public record SaveEducationAchievementRequest(
        Long achievementId,
        @NotBlank(message = "실적 유형을 선택하세요.") String achievementType,
        @NotBlank(message = "관리항목 코드를 입력하세요.") String managementItemCode,
        @NotNull(message = "발생일을 입력하세요.") LocalDate occurrenceDate,
        List<DegreeCompletionStudentDetailRequest> degreeCompletionStudentDetails,
        List<StudentGuidanceDetailRequest> studentGuidanceDetails) {
}

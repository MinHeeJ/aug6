package kr.ac.knue.commonfoundation.educationachievement;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Represents one persisted student-guidance entry owned by a student-guidance achievement.
 */
public record StudentGuidanceDetail(
        @NotBlank(message = "지도학생명을 입력하세요.") String guidanceStudentName,
        @NotNull(message = "지도 시작일을 입력하세요.") LocalDate guidanceStartDate,
        @NotNull(message = "지도 종료일을 입력하세요.") LocalDate guidanceEndDate,
        @NotNull(message = "학생수를 입력하세요.") @Min(value = 1, message = "학생수는 1명 이상이어야 합니다.") Integer studentCount) {
}

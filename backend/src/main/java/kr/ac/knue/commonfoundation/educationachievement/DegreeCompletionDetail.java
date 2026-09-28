package kr.ac.knue.commonfoundation.educationachievement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * Represents one degree-completion student detail stored with a degree achievement master.
 */
public record DegreeCompletionDetail(
        @NotBlank(message = "학위구분을 선택하세요.")
        @Pattern(regexp = "MASTER|DOCTOR", message = "학위구분은 MASTER 또는 DOCTOR여야 합니다.") String degreeType,
        @NotBlank(message = "학생명을 입력하세요.") String studentName,
        @NotBlank(message = "논문제목을 입력하세요.") String thesisTitle,
        @NotNull(message = "수여일을 입력하세요.") LocalDate degreeAwardedDate) {
}

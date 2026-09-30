package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Binds one required guided-student detail within a degree-completion save request. */
public record DegreeCompletionStudentRequest(
        @NotBlank(message = "학위구분을 입력하세요.") String degreeType,
        @NotBlank(message = "학생명을 입력하세요.") String studentName,
        @NotBlank(message = "논문 제목을 입력하세요.") String thesisTitle,
        @NotNull(message = "학위수여일을 입력하세요.") LocalDate degreeAwardedDate) {
}

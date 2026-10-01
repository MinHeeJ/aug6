package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Request detail for one student included in a master's or doctoral completion achievement. */
public record DegreeCompletionStudentRequest(
        @NotBlank(message = "학위구분을 입력하세요.") String degreeType,
        @NotBlank(message = "학생명을 입력하세요.") String studentName,
        String thesisTitle,
        @NotNull(message = "학위수여일을 입력하세요.") LocalDate degreeAwardedDate
) {
}

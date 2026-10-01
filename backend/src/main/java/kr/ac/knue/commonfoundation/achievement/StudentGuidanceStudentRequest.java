package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.constraints.NotBlank;

/** Input row for a student included in a student-guidance achievement. */
public record StudentGuidanceStudentRequest(
        String studentNo,
        @NotBlank(message = "지도학생명을 입력하세요.") String studentName
) {
}

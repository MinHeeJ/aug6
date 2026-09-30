package kr.ac.knue.commonfoundation.achievement;
import jakarta.validation.constraints.NotBlank;
/** Input detail validated before any student-guidance header or child row is created. */
public record StudentGuidanceStudentRequest(@NotBlank String studentNo, @NotBlank String studentName, @NotBlank String guidanceType) {}

package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import jakarta.validation.constraints.*;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;

/** Typed input for create and path-identified update; identity and owner are server controlled. */
public record EmploymentRateImprovementRequest(
        @NotBlank String managementItemCode,
        @NotNull LocalDate achievementDate,
        @Pattern(regexp = "[0-9]{4}") String evaluationYear,
        @NotNull LocalDate specialLectureStartDate,
        @NotNull LocalDate specialLectureEndDate,
        @NotBlank @Size(max = 500) String mockExamQuestionPeriod,
        @Size(max = 300) String attachmentRef,
        EducationAchievementStatus achievementStatus) {
}

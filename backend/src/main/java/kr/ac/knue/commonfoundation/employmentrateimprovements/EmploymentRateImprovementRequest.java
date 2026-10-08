package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Minimal approved teacher inputs; identity, owner, year and status are server controlled. */
public record EmploymentRateImprovementRequest(
        @NotBlank String managementItemCode,
        @NotNull LocalDate achievementDate,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        @Size(max = 10000) String mockExamQuestionPeriod,
        @Size(max = 300) String attachmentRef) {
}

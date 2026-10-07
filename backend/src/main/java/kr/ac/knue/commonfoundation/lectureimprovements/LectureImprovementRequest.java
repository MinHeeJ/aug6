package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** Typed client-editable fields; identity, owner, year and status remain server-controlled. */
public record LectureImprovementRequest(
        @NotBlank String managementItemCode,
        @NotNull LocalDate achievementDate,
        @NotBlank String achievementContent,
        @NotNull @Min(2000) Integer academicYear,
        @NotNull @Min(1) @Max(2) Integer semester,
        List<@NotBlank String> attachmentIds) {
}

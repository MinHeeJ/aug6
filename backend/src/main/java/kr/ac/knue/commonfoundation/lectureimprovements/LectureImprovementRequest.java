package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** Approved request fields; identity and evaluation year are always server-owned. */
public record LectureImprovementRequest(
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @NotBlank String achievementContent,
        @NotNull @Min(2000) Integer academicYear,
        @NotNull @Min(1) @Max(2) Integer semester,
        List<String> attachmentIds) {
}

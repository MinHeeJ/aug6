package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDate;
import java.util.List;

/** Approved command schema; identity and lifecycle state remain exclusively server-owned. */
public record LectureImprovementRequest(
        @NotBlank String managementItemCode,
        @NotNull LocalDate achievementDate,
        @NotNull @Min(2000) Integer academicYear,
        @NotNull @Min(1) @Max(2) Integer semester,
        @NotBlank String achievementContent,
        List<@NotBlank String> attachmentIds) {
}

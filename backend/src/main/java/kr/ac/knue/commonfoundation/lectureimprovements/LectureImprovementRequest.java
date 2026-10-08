package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** Canonical create/update fields; the URL alone selects an existing achievement. */
public record LectureImprovementRequest(
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @NotBlank @JsonAlias("performanceContent") String achievementContent,
        @NotNull @Min(2000) @Max(9999) Integer academicYear,
        @NotNull @Min(1) @Max(2) Integer semester,
        List<@NotBlank @Size(max = 200) String> attachmentIds,
        @Pattern(regexp = "[0-9]{4}") String evaluationYear) {
}

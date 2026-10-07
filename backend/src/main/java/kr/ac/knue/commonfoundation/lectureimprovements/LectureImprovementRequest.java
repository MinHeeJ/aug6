package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Editable fields only; identity, owner, evaluation year and status remain server controlled. */
public record LectureImprovementRequest(
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @JsonAlias("achievementContent") @NotBlank String performanceContent,
        @NotBlank @Pattern(regexp = "[0-9]{4}") String academicYear,
        @NotBlank @Pattern(regexp = "[12]") String semester,
        @Size(max = 300) String attachmentRef,
        List<@NotBlank @Size(max = 300) String> attachmentIds) {
}

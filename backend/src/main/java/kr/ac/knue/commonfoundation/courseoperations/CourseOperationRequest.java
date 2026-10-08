package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Create/update input; only the PUT path selects a row and evaluation year remains immutable. */
public record CourseOperationRequest(
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @NotBlank String performanceDetails,
        @Size(max = 100) List<@NotBlank @Size(max = 200) String> attachmentIds,
        @Pattern(regexp = "[0-9]{4}") String evaluationYear) {
}

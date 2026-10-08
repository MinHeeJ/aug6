package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Create/update input; only the PUT path owns the existing achievement identity. */
public record CourseOperationRequest(
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @NotBlank String performanceDetails,
        List<@NotBlank String> attachmentIds) {
}

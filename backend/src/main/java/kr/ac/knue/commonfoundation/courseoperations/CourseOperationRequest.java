package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Approved create/update payload; identity belongs exclusively to the update URL. */
public record CourseOperationRequest(
        @NotBlank String managementItemCode,
        @NotNull LocalDate achievementDate,
        @NotBlank String performanceDetails,
        List<String> attachmentIds) {
}

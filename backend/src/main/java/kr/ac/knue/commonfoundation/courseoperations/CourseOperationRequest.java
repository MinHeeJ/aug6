package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Approved form fields; identity, owner and lifecycle state are deliberately server controlled. */
public record CourseOperationRequest(
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @NotBlank String performanceDetails,
        @Size(max = 300) String attachmentRef,
        @Size(max = 20) List<@NotBlank @Size(max = 200) String> attachmentIds) {
}

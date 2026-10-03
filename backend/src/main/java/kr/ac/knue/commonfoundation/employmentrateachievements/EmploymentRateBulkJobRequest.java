package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** Request for a policy-controlled employment-rate bulk operation. */
public record EmploymentRateBulkJobRequest(
        @NotBlank String evaluationYear,
        @NotBlank String actionType,
        @NotNull Map<String, Object> targetCondition) {
}

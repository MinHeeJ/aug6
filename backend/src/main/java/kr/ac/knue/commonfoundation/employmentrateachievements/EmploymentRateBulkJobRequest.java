package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

/** Conditions are retained for preview only until the bulk policy is approved. */
public record EmploymentRateBulkJobRequest(
        @NotBlank @Pattern(regexp = "[0-9]{4}") String evaluationYear,
        @NotBlank @Pattern(regexp = "GENERATE|DELETE") String actionType,
        JsonNode targetCondition,
        Boolean confirmed) {
}

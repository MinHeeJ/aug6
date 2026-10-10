package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Validates the shape of a bulk request without treating it as policy approval. */
public record EmploymentRateBulkJobRequest(
        @NotBlank @Pattern(regexp = "[0-9]{4}") String evaluationYear,
        @NotBlank @Pattern(regexp = "GENERATE|DELETE") String actionType,
        @NotNull JsonNode targetConditionJson) {
}

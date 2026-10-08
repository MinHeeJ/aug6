package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Minimal bulk request; admission never implies approval of the execution policy. */
public record EmploymentRateBulkJobRequest(
        @NotBlank @Pattern(regexp = "[0-9]{4}") String evaluationYear,
        @NotBlank @Pattern(regexp = "GENERATE|DELETE") String actionType,
        JsonNode targetCondition,
        Boolean confirmed) {
}

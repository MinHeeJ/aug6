package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

/** Requested conditions are not an approved eligibility policy or a server confirmation token. */
public record EmploymentRateBulkJobRequest(
        @NotBlank @Pattern(regexp = "[0-9]{4}") String evaluationYear,
        @NotBlank @Pattern(regexp = "GENERATE|DELETE") String actionType,
        Map<String, Object> targetCondition) {
}

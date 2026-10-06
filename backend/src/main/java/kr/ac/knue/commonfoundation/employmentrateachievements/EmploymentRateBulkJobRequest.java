package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Approved execution conditions; this DTO does not imply that the pending policy is approved.
 * Accepts the decision ledger's targetConditionJson spelling without breaking the existing
 * OpenAPI/UI targetCondition field or the service's record accessor.
 */
public record EmploymentRateBulkJobRequest(
        @NotBlank(message = "평가연도를 입력하세요.") @Pattern(regexp = "[0-9]{4}") String evaluationYear,
        @NotBlank(message = "작업 유형을 입력하세요.") @Pattern(regexp = "GENERATE|DELETE") String actionType,
        @JsonAlias("targetConditionJson") JsonNode targetCondition) {
}

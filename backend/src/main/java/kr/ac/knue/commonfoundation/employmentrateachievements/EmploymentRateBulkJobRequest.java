package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

/** Conditions for a policy-gated bulk command, not authorization or an executable target list. */
public record EmploymentRateBulkJobRequest(
        @NotBlank(message = "평가년도를 입력하세요.") String evaluationYear,
        @NotBlank @Pattern(regexp = "GENERATE|DELETE") String actionType,
        Map<String, Object> targetCondition) {
}

package kr.ac.knue.commonfoundation.basic83;

import jakarta.validation.constraints.NotBlank;

/** Represents the still policy-gated bulk execution request without deciding its execution rules. */
public record EmploymentRateBulkJobRequest(
        @NotBlank(message = "평가연도를 입력하세요.") String evaluationYear,
        @NotBlank(message = "작업유형을 입력하세요.") String actionType,
        String targetCondition) {
}

package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

/** Request contract for a policy-approved employment-rate bulk operation. */
public record EmploymentRateBulkJobRequest(
        @NotBlank(message = "평가연도를 입력하세요.") String evaluationYear,
        @NotBlank(message = "일괄 작업 유형을 입력하세요.")
        @Pattern(regexp = "GENERATE|DELETE", message = "일괄 작업 유형은 GENERATE 또는 DELETE여야 합니다.")
        String actionType,
        Map<String, Object> targetCondition) {
}

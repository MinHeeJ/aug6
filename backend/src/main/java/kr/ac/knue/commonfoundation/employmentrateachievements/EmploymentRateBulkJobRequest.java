package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

/** R07 request for a policy-approved employment-rate batch action. */
public record EmploymentRateBulkJobRequest(
        @NotBlank(message = "평가연도는 필수입니다.")
        @Pattern(regexp = "^[0-9]{4}$", message = "평가연도는 YYYY 형식이어야 합니다.") String evaluationYear,
        @NotBlank(message = "처리유형은 필수입니다.") String actionType,
        Map<String, Object> targetCondition) {
}

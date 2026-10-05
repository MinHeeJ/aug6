package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** Request for a confirmed employment-rate bulk generation or deletion operation. */
public record EmploymentRateBulkJobRequest(
        @NotBlank(message = "평가연도를 입력하세요.") String evaluationYear,
        @NotBlank(message = "처리유형을 입력하세요.") String actionType,
        @NotNull(message = "대상 미리보기 조건을 입력하세요.") Map<String, Object> targetCondition) {
}

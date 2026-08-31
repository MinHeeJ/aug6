package kr.ac.knue.commonfoundation.basic32;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvaluationOrganizationMappingSaveRequest(
        @NotNull(message = "사용자 ID를 입력하세요.") Long userId,
        @NotBlank(message = "조직코드를 입력하세요.") String organizationCode,
        @NotBlank(message = "업무유형을 선택하세요.") String businessType,
        @NotBlank(message = "데이터 범위를 선택하세요.") String dataScope,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}

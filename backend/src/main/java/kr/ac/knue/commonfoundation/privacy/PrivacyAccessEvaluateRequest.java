package kr.ac.knue.commonfoundation.privacy;

import jakarta.validation.constraints.NotBlank;

public record PrivacyAccessEvaluateRequest(
        @NotBlank(message = "역할코드를 선택하세요.") String roleCode,
        @NotBlank(message = "개인정보 필드를 입력하세요.") String fieldKey,
        @NotBlank(message = "접근 유형을 선택하세요.") String accessType,
        @NotBlank(message = "처리 목적을 입력하세요.") String processPurpose) {
}

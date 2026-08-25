package kr.ac.knue.commonfoundation.privacy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrivacyFieldPolicySaveRequest(
        @NotBlank(message = "개인정보 필드를 입력하세요.") String fieldKey,
        @NotBlank(message = "개인정보 등급을 선택하세요.") String privacyGrade,
        @NotBlank(message = "암호화 여부를 선택하세요.") String encryptionRequiredYn,
        @Size(max = 120, message = "마스킹 규칙은 120자 이하여야 합니다.") String maskingRule,
        @NotBlank(message = "일반 로그 제외 여부를 선택하세요.") String logExclusionYn,
        @NotBlank(message = "변경 사유를 입력하세요.") @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason,
        String actualValue,
        String originalValue) {
}

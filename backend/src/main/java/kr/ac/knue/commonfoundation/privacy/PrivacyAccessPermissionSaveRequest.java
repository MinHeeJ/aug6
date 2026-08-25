package kr.ac.knue.commonfoundation.privacy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrivacyAccessPermissionSaveRequest(
        @NotBlank(message = "역할코드를 선택하세요.") String roleCode,
        @NotBlank(message = "개인정보 필드를 입력하세요.") String fieldKey,
        @NotBlank(message = "원문 조회 권한을 선택하세요.") String rawViewAllowedYn,
        @NotBlank(message = "마스킹 조회 권한을 선택하세요.") String maskedViewAllowedYn,
        @NotBlank(message = "출력 권한을 선택하세요.") String exportAllowedYn,
        @NotBlank(message = "계좌정보 조회 권한을 선택하세요.") String accountViewAllowedYn,
        @NotBlank(message = "변경 사유를 입력하세요.") @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason,
        Long userId) {
}

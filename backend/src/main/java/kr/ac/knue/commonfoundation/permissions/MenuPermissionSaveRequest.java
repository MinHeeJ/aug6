package kr.ac.knue.commonfoundation.permissions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MenuPermissionSaveRequest(
        @NotBlank(message = "대상 유형을 선택하세요.") String targetType,
        @NotBlank(message = "대상 식별자를 입력하세요.") String targetId,
        @NotNull(message = "메뉴를 선택하세요.") Long menuId,
        @NotBlank(message = "접근 허용 여부를 선택하세요.") String accessAllowed,
        @NotBlank(message = "변경 사유를 입력하세요.") @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason) {
}

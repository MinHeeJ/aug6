package kr.ac.knue.commonfoundation.functionpermissions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FunctionPermissionSaveRequest(
        @NotBlank(message = "화면 ID를 입력하세요.") String screenId,
        @NotBlank(message = "역할 코드를 선택하세요.") String roleCode,
        @NotBlank(message = "기능구분을 선택하세요.") String functionType,
        @NotBlank(message = "허용 여부를 선택하세요.") String permissionAllowed,
        @NotBlank(message = "변경 사유를 입력하세요.") @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason) {
}

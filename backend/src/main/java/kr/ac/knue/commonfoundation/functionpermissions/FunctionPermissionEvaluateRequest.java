package kr.ac.knue.commonfoundation.functionpermissions;

import jakarta.validation.constraints.NotBlank;

public record FunctionPermissionEvaluateRequest(
        @NotBlank(message = "화면 ID를 입력하세요.") String screenId,
        @NotBlank(message = "역할 코드를 선택하세요.") String roleCode,
        @NotBlank(message = "기능구분을 선택하세요.") String functionType,
        @NotBlank(message = "대상 데이터 상태를 입력하세요.") String targetDataStatus,
        String dataScopeRef) {
}

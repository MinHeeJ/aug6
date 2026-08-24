package kr.ac.knue.commonfoundation.functionpermissions;

public record FunctionPermissionEvaluateResponse(
        boolean allowed,
        String screenId,
        String roleCode,
        String functionType,
        String reason) {
}

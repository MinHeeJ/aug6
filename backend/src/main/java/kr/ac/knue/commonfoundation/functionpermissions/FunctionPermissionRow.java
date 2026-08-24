package kr.ac.knue.commonfoundation.functionpermissions;

import java.time.LocalDateTime;

public record FunctionPermissionRow(
        Long functionPermissionId,
        String screenId,
        String screenName,
        String roleCode,
        String roleName,
        String functionType,
        String permissionAllowed,
        String changeReason,
        LocalDateTime updatedAt) {
}

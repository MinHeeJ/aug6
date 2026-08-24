package kr.ac.knue.commonfoundation.periodpermissions;

import java.time.LocalDateTime;

public record PeriodPermissionRow(
        Long periodPermissionLinkId,
        String businessPeriodId,
        Long functionPermissionId,
        String screenId,
        String screenName,
        String roleCode,
        String roleName,
        String functionType,
        String permissionAllowed,
        LocalDateTime effectiveStartAt,
        LocalDateTime effectiveEndAt,
        String periodState,
        Boolean effectiveAllowed,
        String changeReason,
        LocalDateTime updatedAt) {
}

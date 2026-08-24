package kr.ac.knue.commonfoundation.permissionops;

import java.time.LocalDateTime;

public record PermissionChangeHistoryRow(
        Long permissionHistoryId,
        String targetType,
        String targetId,
        String beforeValue,
        String afterValue,
        Long changedBy,
        String reason,
        LocalDateTime changedAt
) {
}

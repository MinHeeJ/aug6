package kr.ac.knue.commonfoundation.audit;

import java.time.LocalDateTime;

public record PermissionChangeLogRow(
        Long permissionHistoryId,
        String targetType,
        String targetId,
        String beforeValue,
        String afterValue,
        Long approverUserId,
        String approverLoginId,
        String approverName,
        Long changedBy,
        String changerLoginId,
        String changerName,
        String reason,
        LocalDateTime changedAt) {
}

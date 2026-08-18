package kr.ac.knue.commonfoundation.permissions;

import java.time.LocalDateTime;

public record MenuPermissionRow(
        Long permissionId,
        String targetType,
        String targetId,
        String targetName,
        Long menuId,
        String topMenuName,
        String middleMenuName,
        String screenMenuName,
        String screenId,
        String url,
        String accessAllowed,
        String status,
        String changeReason,
        LocalDateTime updatedAt) {
}

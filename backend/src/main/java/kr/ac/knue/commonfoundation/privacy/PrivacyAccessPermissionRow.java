package kr.ac.knue.commonfoundation.privacy;

import java.time.LocalDateTime;

public record PrivacyAccessPermissionRow(
        Long permissionId,
        String roleCode,
        String roleName,
        String fieldKey,
        String rawViewAllowedYn,
        String maskedViewAllowedYn,
        String exportAllowedYn,
        String accountViewAllowedYn,
        String changeReason,
        LocalDateTime updatedAt,
        Long updatedBy) {
}

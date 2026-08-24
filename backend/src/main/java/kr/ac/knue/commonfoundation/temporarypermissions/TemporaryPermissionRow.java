package kr.ac.knue.commonfoundation.temporarypermissions;

import java.time.LocalDateTime;

public record TemporaryPermissionRow(
        Long temporaryPermissionId,
        Long userId,
        String userName,
        String workDataRef,
        String functionType,
        LocalDateTime validStartAt,
        LocalDateTime validEndAt,
        String status,
        String changeReason,
        LocalDateTime updatedAt) {
}

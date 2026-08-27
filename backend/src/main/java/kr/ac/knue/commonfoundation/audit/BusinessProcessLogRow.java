package kr.ac.knue.commonfoundation.audit;

import java.time.LocalDateTime;

public record BusinessProcessLogRow(
        Long auditLogId,
        String actionType,
        String targetKey,
        String beforeState,
        String afterState,
        Long actorUserId,
        String actorLoginId,
        String actorName,
        String resultStatus,
        String requestId,
        LocalDateTime createdAt) {
}

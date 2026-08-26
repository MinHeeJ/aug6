package kr.ac.knue.commonfoundation.batch;

import java.time.LocalDateTime;

public record BatchRetryTargetRow(
        String originalExecutionId,
        String batchId,
        String executionStatus,
        String failedItemKey,
        String failureReason,
        LocalDateTime startedAt,
        LocalDateTime endedAt) {
}

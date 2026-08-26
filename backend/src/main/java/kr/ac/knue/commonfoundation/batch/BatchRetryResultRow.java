package kr.ac.knue.commonfoundation.batch;

import java.time.LocalDateTime;

public record BatchRetryResultRow(
        String retryExecutionId,
        String originalExecutionId,
        String failedItemKey,
        String retryReason,
        String requestId,
        LocalDateTime createdAt,
        Long createdBy) {
}

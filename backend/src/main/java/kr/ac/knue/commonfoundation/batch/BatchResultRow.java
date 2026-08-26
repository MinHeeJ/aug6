package kr.ac.knue.commonfoundation.batch;

import java.time.LocalDateTime;

public record BatchResultRow(
        String executionId,
        String batchId,
        String batchType,
        String executionStatus,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer totalCount,
        Integer successCount,
        Integer failureCount,
        Integer excludedCount,
        Long elapsedMillis,
        boolean hasLog) {
}

package kr.ac.knue.commonfoundation.basic46;

import java.time.LocalDateTime;

public record BatchProcessingResultRow(
        String batchId,
        String batchType,
        String evaluationYear,
        String areaCode,
        String organizationCode,
        Long targetUserId,
        String targetConditionSummary,
        int totalCount,
        int successCount,
        int failureCount,
        int excludedCount,
        String jobStatus,
        Long requestedBy,
        LocalDateTime requestedAt,
        String requestId) {
}

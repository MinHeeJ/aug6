package kr.ac.knue.commonfoundation.basic54;

import java.time.LocalDateTime;

public record BulkReportJobRow(Long jobId, String reportId, String reportName, Long requesterId, String targetHash,
                               String status, int progressRate, int totalCount, int successCount, int failCount,
                               String resultFileRef, String requestId, LocalDateTime requestedAt,
                               LocalDateTime completedAt) {
}

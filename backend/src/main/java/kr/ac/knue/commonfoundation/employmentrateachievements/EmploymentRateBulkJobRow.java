package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDateTime;

/**
 * Materialized batch-job header used to provide a retrievable result after an
 * accepted employment-rate bulk request.
 */
public record EmploymentRateBulkJobRow(
        String jobId,
        String evaluationYear,
        String actionType,
        String jobStatus,
        int totalCount,
        int processedCount,
        int unprocessedCount,
        LocalDateTime requestedAt) {
}

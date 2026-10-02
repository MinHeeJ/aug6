package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDateTime;

/** Materialized per-target result for an employment-rate bulk job. */
public record EmploymentRateBulkJobItemRow(
        Long jobItemId,
        Long targetUserId,
        String processingStatus,
        String resultMessage,
        LocalDateTime processedAt) {
}

package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Read model preserving an R07 batch request and per-target processing outcome. */
public record EmploymentRateBulkJobRow(
        String batchJobId,
        String evaluationYear,
        Map<String, Object> targetCondition,
        String actionType,
        String jobStatus,
        int totalCount,
        int successCount,
        int failureCount,
        int excludedCount,
        LocalDateTime requestedAt,
        List<EmploymentRateBulkJobItem> items) {
    public record EmploymentRateBulkJobItem(Long targetUserId, boolean processed, String unprocessedReason) {
    }
}

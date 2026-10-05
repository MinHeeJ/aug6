package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDateTime;

/** Read model for a recorded employment-rate bulk job and its aggregate outcome. */
public record EmploymentRateBulkJobResponse(
        String batchJobId,
        String evaluationYear,
        String actionType,
        String jobStatus,
        int targetCount,
        int processedCount,
        int successCount,
        int failureCount,
        LocalDateTime requestedAt,
        LocalDateTime completedAt) {
}

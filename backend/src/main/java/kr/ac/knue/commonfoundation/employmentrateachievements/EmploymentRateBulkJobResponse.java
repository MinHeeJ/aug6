package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDateTime;
import java.util.List;

/** Result model for a persisted employment-rate bulk job and its per-target outcomes. */
public record EmploymentRateBulkJobResponse(
        String jobId,
        String evaluationYear,
        String actionType,
        String jobStatus,
        int totalCount,
        int processedCount,
        int unprocessedCount,
        LocalDateTime requestedAt,
        List<EmploymentRateBulkJobItem> items) {
}

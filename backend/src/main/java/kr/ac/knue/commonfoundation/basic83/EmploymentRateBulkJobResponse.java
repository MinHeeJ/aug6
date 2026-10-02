package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDateTime;
import java.util.List;

/** Read model for a persisted employment-rate bulk job and its item outcomes. */
public record EmploymentRateBulkJobResponse(
        String jobId,
        String evaluationYear,
        String actionType,
        String status,
        Long requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        long totalCount,
        long processedCount,
        long skippedCount,
        long failedCount,
        List<EmploymentRateBulkJobItemRow> items) {
}

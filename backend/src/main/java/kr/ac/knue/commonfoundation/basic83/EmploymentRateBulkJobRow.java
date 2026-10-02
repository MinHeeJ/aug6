package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDateTime;

/** Persistent header of an employment-rate bulk job. */
public record EmploymentRateBulkJobRow(
        String jobId,
        String evaluationYear,
        String actionType,
        String status,
        Long requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime completedAt) {
}

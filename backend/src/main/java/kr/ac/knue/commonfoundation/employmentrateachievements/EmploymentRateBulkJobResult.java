package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;

/** Read model for an employment-rate bulk job and its individual processing outcomes. */
public record EmploymentRateBulkJobResult(
        String jobId,
        String evaluationYear,
        String actionType,
        String status,
        int processedCount,
        int unprocessedCount,
        List<EmploymentRateBulkJobItem> items) {
}

package kr.ac.knue.commonfoundation.employmentrateachievements;

/** Result summary for an employment-rate bulk job and its target-level outcomes. */
public record EmploymentRateBulkJobResult(
        String jobId,
        String evaluationYear,
        String actionType,
        String status,
        int totalCount,
        int processedCount,
        int unprocessedCount) {
}

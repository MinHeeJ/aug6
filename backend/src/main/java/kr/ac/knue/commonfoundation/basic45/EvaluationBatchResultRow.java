package kr.ac.knue.commonfoundation.basic45;

public record EvaluationBatchResultRow(
        String batchId,
        String jobType,
        String jobTypeName,
        String requestStatus,
        String targetCondition,
        int totalCount,
        int successCount,
        int failureCount,
        int excludedCount,
        String requestId,
        String requestedAt,
        String completedAt) {
}

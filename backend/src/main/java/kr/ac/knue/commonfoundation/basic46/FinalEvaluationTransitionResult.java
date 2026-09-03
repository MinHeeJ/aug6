package kr.ac.knue.commonfoundation.basic46;

public record FinalEvaluationTransitionResult(
        String finalizationBatchId,
        Long targetUserId,
        String evaluationYear,
        String actionType,
        String finalStatus,
        int totalCount,
        int successCount,
        int failureCount,
        int excludedCount,
        String snapshotRef,
        String requestId) {
}

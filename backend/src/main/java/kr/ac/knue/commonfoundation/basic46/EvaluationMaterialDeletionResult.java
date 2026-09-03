package kr.ac.knue.commonfoundation.basic46;

public record EvaluationMaterialDeletionResult(
        String deletionBatchId,
        String evaluationYear,
        String areaCode,
        String generationBatchId,
        int totalCount,
        int successCount,
        int failureCount,
        int excludedCount,
        String requestId) {
}

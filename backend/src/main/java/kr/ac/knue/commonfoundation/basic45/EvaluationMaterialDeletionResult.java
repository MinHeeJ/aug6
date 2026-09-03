package kr.ac.knue.commonfoundation.basic45;

public record EvaluationMaterialDeletionResult(
        String batchId,
        String requestId,
        int targetCount,
        int deletedCount,
        int excludedCount) {
}

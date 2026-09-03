package kr.ac.knue.commonfoundation.basic46;

public record ScoreRecalculationResult(
        String recalculationBatchId,
        String evaluationYear,
        String areaCode,
        Long targetUserId,
        String formulaVersionId,
        int totalCount,
        int successCount,
        int failureCount,
        int excludedCount,
        String requestId) {
}

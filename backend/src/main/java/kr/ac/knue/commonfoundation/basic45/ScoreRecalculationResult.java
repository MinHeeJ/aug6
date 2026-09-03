package kr.ac.knue.commonfoundation.basic45;

public record ScoreRecalculationResult(
        String batchId,
        String requestId,
        int targetCount,
        int recalculatedCount,
        int excludedCount) {
}

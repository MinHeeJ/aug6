package kr.ac.knue.commonfoundation.basic45;

public record EvaluationMaterialGenerationResult(
        String batchId,
        String requestId,
        int targetCount,
        int createdCount,
        int excludedCount) {
}

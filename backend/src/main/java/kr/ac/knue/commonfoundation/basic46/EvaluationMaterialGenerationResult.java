package kr.ac.knue.commonfoundation.basic46;

public record EvaluationMaterialGenerationResult(
        String generationBatchId,
        String evaluationYear,
        String areaCode,
        String organizationCode,
        Long targetUserId,
        int totalCount,
        int successCount,
        int failureCount,
        int excludedCount,
        String requestId) {
}

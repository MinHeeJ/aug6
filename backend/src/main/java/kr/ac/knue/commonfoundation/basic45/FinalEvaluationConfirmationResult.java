package kr.ac.knue.commonfoundation.basic45;

public record FinalEvaluationConfirmationResult(
        String batchId,
        String requestId,
        Long targetId,
        String previousStatus,
        String nextStatus,
        int changedMaterialCount) {
}

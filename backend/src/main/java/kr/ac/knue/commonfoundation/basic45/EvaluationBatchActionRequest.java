package kr.ac.knue.commonfoundation.basic45;

public record EvaluationBatchActionRequest(
        String evaluationYear,
        String areaCode,
        String organizationCode,
        String targetUserId,
        String generationBatchId,
        String deleteReason,
        String formulaVersionId,
        String actionType,
        String cancelReason) {
}

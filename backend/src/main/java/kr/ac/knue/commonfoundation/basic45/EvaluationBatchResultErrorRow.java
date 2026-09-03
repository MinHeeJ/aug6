package kr.ac.knue.commonfoundation.basic45;

public record EvaluationBatchResultErrorRow(
        String batchId,
        String targetKey,
        String targetName,
        String errorCode,
        String message,
        String detail) {
}

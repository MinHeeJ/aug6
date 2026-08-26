package kr.ac.knue.commonfoundation.batch;

public record BatchResultSearchCriteria(
        String executionId,
        String batchId,
        String executionStatus) {
}

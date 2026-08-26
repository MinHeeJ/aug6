package kr.ac.knue.commonfoundation.batch;

public record BatchRetryTargetSearchCriteria(String originalExecutionId, String failedItemKey) {
}

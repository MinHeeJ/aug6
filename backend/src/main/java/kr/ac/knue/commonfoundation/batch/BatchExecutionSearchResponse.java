package kr.ac.knue.commonfoundation.batch;

import java.util.List;

public record BatchExecutionSearchResponse(List<BatchExecutionRow> executions, int page, int size, long totalElements) {
}

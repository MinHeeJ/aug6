package kr.ac.knue.commonfoundation.basic45;

import java.util.List;

public record EvaluationBatchResultErrorListResponse(
        String batchId,
        List<EvaluationBatchResultErrorRow> errors,
        int page,
        int size,
        long totalElements) {
}

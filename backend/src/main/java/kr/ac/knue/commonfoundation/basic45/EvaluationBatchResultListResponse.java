package kr.ac.knue.commonfoundation.basic45;

import java.util.List;

public record EvaluationBatchResultListResponse(
        List<EvaluationBatchResultRow> results,
        int page,
        int size,
        long totalElements) {
}

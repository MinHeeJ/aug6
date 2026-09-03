package kr.ac.knue.commonfoundation.basic46;

import java.util.List;

public record BatchProcessingResultSearchResponse(
        List<BatchProcessingResultRow> results,
        int page,
        int size,
        long totalElements) {
}

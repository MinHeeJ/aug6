package kr.ac.knue.commonfoundation.batch;

import java.util.List;

public record BatchResultSearchResponse(
        List<BatchResultRow> results,
        int page,
        int size,
        long totalElements) {
}

package kr.ac.knue.commonfoundation.basic48;

import java.util.List;

public record ScoreCalculationHistorySearchResponse(
        List<ScoreCalculationHistoryRow> results,
        int page,
        int size,
        long totalElements
) {
}

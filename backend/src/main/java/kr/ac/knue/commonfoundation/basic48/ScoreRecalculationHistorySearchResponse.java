package kr.ac.knue.commonfoundation.basic48;

import java.util.List;

public record ScoreRecalculationHistorySearchResponse(
        List<ScoreRecalculationHistoryRow> results,
        int page,
        int size,
        long totalElements
) {
}

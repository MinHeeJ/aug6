package kr.ac.knue.commonfoundation.basic48;

import java.util.List;

public record ScoreAdjustmentHistorySearchResponse(
        List<ScoreAdjustmentHistoryRow> results,
        int page,
        int size,
        long totalElements
) {
}

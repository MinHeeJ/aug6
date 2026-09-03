package kr.ac.knue.commonfoundation.basic46;

import java.util.List;

public record ScoreRecalculationSearchResponse(
        List<ScoreRecalculationRow> recalculations,
        int page,
        int size,
        long totalElements) {
}

package kr.ac.knue.commonfoundation.basic34;

import java.util.List;

public record EvaluationScoreSearchResponse(
        List<EvaluationScoreRow> evaluationScores,
        int page,
        int pageSize,
        long totalElements) {
}

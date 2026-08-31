package kr.ac.knue.commonfoundation.basic33;

import java.util.List;

public record EvaluationItemSearchResponse(
        List<EvaluationItemRow> evaluationItems,
        int page,
        int size,
        long totalElements) {
}

package kr.ac.knue.commonfoundation.basic33;

import java.util.List;

public record EvaluationAreaSearchResponse(
        List<EvaluationAreaRow> evaluationAreas,
        int page,
        int size,
        long totalElements) {
}

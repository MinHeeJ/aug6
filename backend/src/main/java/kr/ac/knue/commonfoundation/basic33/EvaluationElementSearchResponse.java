package kr.ac.knue.commonfoundation.basic33;

import java.util.List;

public record EvaluationElementSearchResponse(
        List<EvaluationElementRow> evaluationElements,
        int page,
        int size,
        long totalElements) {
}

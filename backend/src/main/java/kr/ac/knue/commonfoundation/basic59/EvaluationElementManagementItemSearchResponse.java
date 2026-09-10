package kr.ac.knue.commonfoundation.basic59;

import java.util.List;

public record EvaluationElementManagementItemSearchResponse(
        List<EvaluationElementManagementItemRow> evaluationElementManagementItems,
        int page,
        int pageSize,
        long totalElements) {
}

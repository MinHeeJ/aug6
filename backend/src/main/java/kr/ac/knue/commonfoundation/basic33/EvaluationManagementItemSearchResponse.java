package kr.ac.knue.commonfoundation.basic33;

import java.util.List;

public record EvaluationManagementItemSearchResponse(
        List<EvaluationManagementItemRow> evaluationManagementItems,
        int page,
        int size,
        long totalElements) {
}

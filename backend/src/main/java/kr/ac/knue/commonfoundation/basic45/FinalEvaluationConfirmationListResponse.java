package kr.ac.knue.commonfoundation.basic45;

import java.util.List;

public record FinalEvaluationConfirmationListResponse(
        List<FinalEvaluationConfirmationTarget> confirmations,
        int page,
        int size,
        long totalElements) {
    public FinalEvaluationConfirmationListResponse {
        confirmations = List.copyOf(confirmations);
    }
}

package kr.ac.knue.commonfoundation.basic46;

import java.util.List;

public record FinalEvaluationConfirmationSearchResponse(
        List<FinalEvaluationConfirmationRow> confirmations,
        int page,
        int size,
        long totalElements) {
}

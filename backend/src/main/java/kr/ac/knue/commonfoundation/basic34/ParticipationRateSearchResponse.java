package kr.ac.knue.commonfoundation.basic34;

import java.util.List;

public record ParticipationRateSearchResponse(
        List<ParticipationRateRow> participationRates,
        int page,
        int pageSize,
        long totalElements) {
}

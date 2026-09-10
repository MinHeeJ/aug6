package kr.ac.knue.commonfoundation.basic59;

import java.util.List;

public record ParticipationRateOperationSettingSearchResponse(
        List<ParticipationRateOperationSettingRow> participationRateOperationSettings,
        int page,
        int pageSize,
        long totalElements) {
}

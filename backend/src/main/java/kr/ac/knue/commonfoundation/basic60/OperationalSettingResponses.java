package kr.ac.knue.commonfoundation.basic60;

import java.util.List;

public final class OperationalSettingResponses {
    private OperationalSettingResponses() {}

    public record EvaluationElementManagementItemSettingSearchResponse(
            List<OperationalSettingRow> evaluationElementManagementItemSettings,
            int page,
            int pageSize,
            long totalElements) {}

    public record ParticipationAllocationRateSettingSearchResponse(
            List<OperationalSettingRow> participationAllocationRateSettings,
            int page,
            int pageSize,
            long totalElements) {}

    public record ManagementItemEvaluationScoreSettingSearchResponse(
            List<OperationalSettingRow> managementItemEvaluationScoreSettings,
            int page,
            int pageSize,
            long totalElements) {}
}

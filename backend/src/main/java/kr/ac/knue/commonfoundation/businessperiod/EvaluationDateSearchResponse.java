package kr.ac.knue.commonfoundation.businessperiod;

import java.util.List;

public record EvaluationDateSearchResponse(
        List<BusinessPeriodSettingRow> evaluationDates,
        int page,
        int pageSize,
        long totalElements
) {}

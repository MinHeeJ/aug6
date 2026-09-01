package kr.ac.knue.commonfoundation.businessperiod;

import java.util.List;

public record BusinessPeriodSearchResponse(
        List<BusinessPeriodSettingRow> businessPeriods,
        int page,
        int pageSize,
        long totalElements
) {}

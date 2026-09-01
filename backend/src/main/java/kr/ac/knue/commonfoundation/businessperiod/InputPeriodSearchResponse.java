package kr.ac.knue.commonfoundation.businessperiod;

import java.util.List;

public record InputPeriodSearchResponse(
        List<BusinessPeriodSettingRow> inputPeriods,
        int page,
        int pageSize,
        long totalElements
) {}

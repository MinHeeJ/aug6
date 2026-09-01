package kr.ac.knue.commonfoundation.businessperiod;

import java.util.List;

public record ModificationPeriodSearchResponse(
        List<BusinessPeriodSettingRow> modificationPeriods,
        int page,
        int pageSize,
        long totalElements
) {}

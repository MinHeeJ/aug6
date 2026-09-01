package kr.ac.knue.commonfoundation.businessperiod;

import java.util.List;

public record DepartmentChairConfirmPeriodSearchResponse(
        List<BusinessPeriodSettingRow> departmentChairConfirmPeriods,
        int page,
        int pageSize,
        long totalElements
) {}

package kr.ac.knue.commonfoundation.resultviewperiod;

import java.util.List;

public record ResultViewPeriodSearchResponse(
        List<ResultViewPeriodRow> resultViewPeriods,
        int page,
        int pageSize,
        long totalElements
) {
}

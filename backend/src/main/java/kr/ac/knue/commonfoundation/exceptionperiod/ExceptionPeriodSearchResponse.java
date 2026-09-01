package kr.ac.knue.commonfoundation.exceptionperiod;

import java.util.List;

public record ExceptionPeriodSearchResponse(
        List<ExceptionPeriodRow> exceptionPeriods,
        int page,
        int pageSize,
        long totalElements
) {
}

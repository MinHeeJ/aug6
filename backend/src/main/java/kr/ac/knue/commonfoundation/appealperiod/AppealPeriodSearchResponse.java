package kr.ac.knue.commonfoundation.appealperiod;

import java.util.List;

public record AppealPeriodSearchResponse(
        List<AppealPeriodRow> appealPeriods,
        int page,
        int pageSize,
        long totalElements
) {
}

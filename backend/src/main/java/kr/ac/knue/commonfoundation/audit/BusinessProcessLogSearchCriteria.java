package kr.ac.knue.commonfoundation.audit;

import java.time.LocalDate;

public record BusinessProcessLogSearchCriteria(
        String filter,
        String actionType,
        String targetKey,
        Long actorUserId,
        String resultStatus,
        LocalDate fromDate,
        LocalDate toDate) {
}

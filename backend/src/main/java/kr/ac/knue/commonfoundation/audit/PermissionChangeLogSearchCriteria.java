package kr.ac.knue.commonfoundation.audit;

import java.time.LocalDate;

public record PermissionChangeLogSearchCriteria(
        String targetId,
        String targetType,
        Long approverUserId,
        Long changedBy,
        LocalDate fromDate,
        LocalDate toDate) {
}

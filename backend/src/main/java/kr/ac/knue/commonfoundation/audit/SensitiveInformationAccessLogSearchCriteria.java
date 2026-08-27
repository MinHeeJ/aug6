package kr.ac.knue.commonfoundation.audit;

import java.time.LocalDate;

public record SensitiveInformationAccessLogSearchCriteria(
        String filter,
        String informationType,
        Long viewerUserId,
        String accessResult,
        LocalDate fromDate,
        LocalDate toDate) {
}

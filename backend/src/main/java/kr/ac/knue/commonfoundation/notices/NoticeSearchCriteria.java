package kr.ac.knue.commonfoundation.notices;

import java.time.LocalDate;

public record NoticeSearchCriteria(
        LocalDate publishStartDate,
        LocalDate publishEndDate,
        String targetRoleCode,
        String targetOrganizationCode,
        Boolean activeOnly) {
}

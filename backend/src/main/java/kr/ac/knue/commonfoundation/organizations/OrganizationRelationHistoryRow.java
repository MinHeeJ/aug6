package kr.ac.knue.commonfoundation.organizations;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrganizationRelationHistoryRow(
        Long historyId,
        Long relationId,
        String organizationCode,
        String beforeParentOrganizationCode,
        String afterParentOrganizationCode,
        LocalDate beforeEffectiveStartDate,
        LocalDate beforeEffectiveEndDate,
        LocalDate afterEffectiveStartDate,
        LocalDate afterEffectiveEndDate,
        LocalDateTime changedAt,
        Long changedBy,
        String changeReason) {
}

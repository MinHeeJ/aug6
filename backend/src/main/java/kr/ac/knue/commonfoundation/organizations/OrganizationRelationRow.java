package kr.ac.knue.commonfoundation.organizations;

import java.time.LocalDate;

public record OrganizationRelationRow(
        Long relationId,
        String organizationCode,
        String parentOrganizationCode,
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate,
        String status,
        String changeReason) {
}

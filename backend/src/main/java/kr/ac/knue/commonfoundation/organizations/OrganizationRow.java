package kr.ac.knue.commonfoundation.organizations;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrganizationRow(
        String organizationCode,
        String organizationName,
        String organizationType,
        String systemUseYn,
        String status,
        String parentOrganizationCode,
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate,
        LocalDateTime updatedAt) {
}

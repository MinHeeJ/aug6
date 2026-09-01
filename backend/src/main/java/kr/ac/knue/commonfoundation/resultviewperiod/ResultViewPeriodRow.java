package kr.ac.knue.commonfoundation.resultviewperiod;

import java.time.LocalDateTime;

public record ResultViewPeriodRow(
        Long settingId,
        String evaluationYear,
        String collegeOrganizationCode,
        String departmentOrganizationCode,
        LocalDateTime viewStartAt,
        LocalDateTime viewEndAt,
        String visibilityScope,
        String activeYn,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String changeReason
) {
}

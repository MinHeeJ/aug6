package kr.ac.knue.commonfoundation.appealperiod;

import java.time.LocalDateTime;

public record AppealPeriodRow(
        Long settingId,
        String evaluationYear,
        String collegeOrganizationCode,
        String departmentOrganizationCode,
        LocalDateTime appealStartAt,
        LocalDateTime appealEndAt,
        Long handlerUserId,
        String activeYn,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String changeReason
) {
}

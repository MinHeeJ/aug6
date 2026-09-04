package kr.ac.knue.commonfoundation.basic50;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BusinessSettingRow(
        Long settingId,
        String evaluationYear,
        String organizationCode,
        String evaluationUnitCode,
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate,
        Long managerUserId,
        String managerName,
        String targetScope,
        String activeYn,
        String changeReason,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

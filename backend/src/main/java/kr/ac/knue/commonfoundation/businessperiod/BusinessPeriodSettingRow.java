package kr.ac.knue.commonfoundation.businessperiod;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BusinessPeriodSettingRow(
        Long settingId,
        String evaluationYear,
        String areaCode,
        String organizationCode,
        String userTypeCode,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDate baseDate,
        String activeYn,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String changeReason
) {}

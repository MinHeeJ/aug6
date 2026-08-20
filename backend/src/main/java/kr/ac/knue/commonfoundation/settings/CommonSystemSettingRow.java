package kr.ac.knue.commonfoundation.settings;

import java.time.LocalDateTime;

public record CommonSystemSettingRow(
        String settingKey,
        String settingValue,
        String unit,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}

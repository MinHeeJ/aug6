package kr.ac.knue.commonfoundation.operations;

import java.time.LocalDateTime;

public record CommonSettingRow(String settingKey, String settingValue, String settingUnit, String changeReason, LocalDateTime updatedAt) {
}

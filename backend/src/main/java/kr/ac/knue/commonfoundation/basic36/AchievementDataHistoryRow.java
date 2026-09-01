package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDateTime;

public record AchievementDataHistoryRow(
        Long historyId,
        String achievementType,
        String achievementKey,
        String changeType,
        String fieldName,
        String beforeValue,
        String afterValue,
        Long changedBy,
        String changedByLoginId,
        String changedByName,
        LocalDateTime changedAt,
        String changeReason) {
}

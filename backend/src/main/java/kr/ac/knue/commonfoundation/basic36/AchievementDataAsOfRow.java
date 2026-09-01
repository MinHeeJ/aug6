package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDateTime;

public record AchievementDataAsOfRow(
        Long snapshotId,
        String achievementType,
        String achievementKey,
        String employeeNo,
        String achievementTitle,
        String achievementStatus,
        String snapshotValue,
        LocalDateTime baseAt,
        LocalDateTime capturedAt) {
}

package kr.ac.knue.commonfoundation.basic32;

import java.time.LocalDateTime;

public record DataChangeHistoryRow(
        Long historyId,
        String targetBusiness,
        String targetKey,
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

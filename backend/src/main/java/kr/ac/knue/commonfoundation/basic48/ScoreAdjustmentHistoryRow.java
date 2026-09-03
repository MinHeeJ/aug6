package kr.ac.knue.commonfoundation.basic48;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScoreAdjustmentHistoryRow(
        String adjustmentHistId,
        Long targetUserId,
        String targetUserName,
        String evaluationYear,
        String areaCode,
        String managementItemCode,
        String adjustmentTarget,
        BigDecimal beforeValue,
        BigDecimal afterValue,
        String adjustmentReason,
        String adjustedByName,
        String approvedByName,
        LocalDateTime adjustedAt,
        String requestId
) {
}

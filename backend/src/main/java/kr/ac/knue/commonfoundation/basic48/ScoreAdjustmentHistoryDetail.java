package kr.ac.knue.commonfoundation.basic48;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScoreAdjustmentHistoryDetail(
        String adjustmentHistId,
        Long targetUserId,
        String targetUserName,
        String evaluationYear,
        String areaCode,
        String areaName,
        String managementItemCode,
        String adjustmentTarget,
        BigDecimal beforeValue,
        BigDecimal afterValue,
        String adjustmentReason,
        String adjustmentRemark,
        String adjustedByName,
        String approvedByName,
        LocalDateTime adjustedAt,
        LocalDateTime approvedAt,
        String approvalTrace,
        String requestId,
        String readOnlyNotice
) {
}

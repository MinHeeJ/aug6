package kr.ac.knue.commonfoundation.basic48;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScoreRecalculationHistoryRow(
        String recalcHistId,
        String jobId,
        Long targetUserId,
        String targetUserName,
        String evaluationYear,
        String formulaVersionId,
        String targetScope,
        Integer changedCount,
        BigDecimal beforeTotalScore,
        BigDecimal afterTotalScore,
        LocalDateTime executedAt,
        String requestId
) {
}

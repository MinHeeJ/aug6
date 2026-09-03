package kr.ac.knue.commonfoundation.basic46;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FinalEvaluationConfirmationRow(
        Long targetUserId,
        String evaluationYear,
        BigDecimal finalScore,
        String latestRecalculationBatchId,
        String latestRecalculationStatus,
        String finalStatus,
        Long confirmedBy,
        LocalDateTime confirmedAt,
        Long canceledBy,
        LocalDateTime canceledAt,
        String cancelReason,
        String snapshotRef,
        Integer materialCount,
        Integer confirmedMaterialCount) {
}

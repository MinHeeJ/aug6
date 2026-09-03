package kr.ac.knue.commonfoundation.basic48;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScoreCalculationHistoryRow(
        String calcHistId,
        Long targetUserId,
        String targetUserName,
        String evaluationYear,
        String areaCode,
        Long sourceAchievementId,
        String managementItemCode,
        BigDecimal baseScore,
        String participationType,
        BigDecimal distributionRate,
        String capAppliedYn,
        String formulaVersionId,
        int generationNo,
        BigDecimal calculatedScore,
        String requestId,
        LocalDateTime calculatedAt
) {
}

package kr.ac.knue.commonfoundation.basic48;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScoreCalculationHistoryDetail(
        String calcHistId,
        Long targetUserId,
        String targetUserName,
        String evaluationYear,
        String areaCode,
        String areaName,
        Long sourceAchievementId,
        String sourceAchievementTitle,
        String managementItemCode,
        BigDecimal baseScore,
        String participationType,
        BigDecimal distributionRate,
        String capAppliedYn,
        String formulaVersionId,
        int generationNo,
        BigDecimal calculatedScore,
        String calculationStepsJson,
        String sourceAchievementLink,
        String requestId,
        LocalDateTime calculatedAt,
        String readOnlyNotice
) {
}

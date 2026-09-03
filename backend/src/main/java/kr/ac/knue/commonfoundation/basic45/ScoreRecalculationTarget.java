package kr.ac.knue.commonfoundation.basic45;

import java.math.BigDecimal;

public record ScoreRecalculationTarget(
        Long evaluationMaterialId,
        String evaluationYear,
        String areaCode,
        Long targetUserId,
        Long sourceAchievementId,
        String generationBatchId,
        Long formulaVersionId,
        Long ruleVersionId,
        String formulaCode,
        BigDecimal beforeScore,
        BigDecimal afterScore,
        Integer nextGenerationNo,
        String materialStatus,
        String achievementTitle) {
}

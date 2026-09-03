package kr.ac.knue.commonfoundation.basic46;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScoreRecalculationRow(
        Long evaluationMaterialId,
        String evaluationYear,
        String areaCode,
        String organizationCode,
        Long targetUserId,
        Long sourceAchievementId,
        String materialStatus,
        BigDecimal previousScore,
        BigDecimal recalculatedScore,
        Long formulaVersionId,
        Integer generationNo,
        String recalculationBatchId,
        String selectionReason,
        String excludedReason,
        LocalDateTime calculatedAt) {
}

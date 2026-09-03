package kr.ac.knue.commonfoundation.basic46;

import java.math.BigDecimal;

public record ScoreRecalculationCandidate(
        Long evaluationMaterialId,
        String evaluationYear,
        String areaCode,
        String organizationCode,
        Long targetUserId,
        Long sourceAchievementId,
        String materialStatus,
        String deletedYn,
        BigDecimal baseScore,
        BigDecimal currentScore,
        String participationType,
        BigDecimal distributionRate,
        String capAppliedYn,
        Integer nextGenerationNo) {
    public boolean canRecalculate() {
        return "N".equals(deletedYn) && !"EVALUATION_CONFIRMED".equals(materialStatus);
    }

    public String excludedReason() {
        if (!"N".equals(deletedYn)) {
            return "삭제 표시 평가자료는 재계산 대상에서 제외됩니다.";
        }
        if ("EVALUATION_CONFIRMED".equals(materialStatus)) {
            return "평가확정 자료는 확정취소 후 재계산할 수 있습니다.";
        }
        return null;
    }
}

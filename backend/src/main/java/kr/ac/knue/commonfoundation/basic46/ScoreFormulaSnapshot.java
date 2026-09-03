package kr.ac.knue.commonfoundation.basic46;

import java.math.BigDecimal;

public record ScoreFormulaSnapshot(
        Long formulaVersionId,
        String calculationType,
        BigDecimal lowerBoundScore,
        BigDecimal upperBoundScore) {
}

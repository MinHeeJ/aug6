package kr.ac.knue.commonfoundation.basic50;

import java.math.BigDecimal;

public record PersonalScoreItem(
        Long scoreId,
        String areaCode,
        String areaName,
        String itemCode,
        String itemName,
        BigDecimal score,
        String calculationDetail,
        String ruleCode,
        String ruleName,
        String evidenceUrl) {
}

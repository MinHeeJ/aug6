package kr.ac.knue.commonfoundation.basic50;

import java.time.LocalDateTime;

public record ResearchCriterionRow(
        Long criterionId,
        String areaCode,
        String areaName,
        String managementCriterionCode,
        String managementCriterionName,
        String parentCriterionCode,
        String activeYn,
        String changeReason,
        Long classifiedAchievementCount,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

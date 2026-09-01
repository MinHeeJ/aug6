package kr.ac.knue.commonfoundation.basic34;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EvaluationScoreRow(
        Long scoreRuleId,
        Long ruleVersionId,
        String versionCode,
        String versionStatus,
        Long managementItemId,
        String areaCode,
        String areaName,
        String itemCode,
        String itemName,
        String evaluationYear,
        String elementCode,
        String elementName,
        String managementItemCode,
        String managementItemName,
        String organizationCode,
        String organizationName,
        BigDecimal baseScore,
        BigDecimal maxScore,
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate,
        String activeYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
    @JsonProperty("ruleVersionStatus")
    public String ruleVersionStatus() {
        return versionStatus;
    }
}

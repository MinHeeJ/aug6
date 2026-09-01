package kr.ac.knue.commonfoundation.basic34;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CalculationFormulaRow(
        Long formulaVersionId,
        Long ruleVersionId,
        String versionCode,
        String versionStatus,
        String formulaCode,
        String calculationType,
        String calculationTypeName,
        String variableDefinition,
        String roundingRule,
        BigDecimal lowerBoundScore,
        BigDecimal upperBoundScore,
        String evaluationYear,
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

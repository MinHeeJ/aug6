package kr.ac.knue.commonfoundation.basic34;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveCalculationFormulaRequest(
        @NotNull(message = "규정버전을 선택하세요.") Long ruleVersionId,
        @NotBlank(message = "산식 ID를 입력하세요.") String formulaCode,
        @NotBlank(message = "계산 유형을 선택하세요.") String calculationType,
        @NotBlank(message = "변수 정의를 입력하세요.") String variableDefinition,
        @NotBlank(message = "반올림 기준을 입력하세요.") String roundingRule,
        BigDecimal lowerBoundScore,
        BigDecimal upperBoundScore,
        @NotBlank(message = "적용연도를 입력하세요.") String evaluationYear,
        @NotNull(message = "적용시작일을 입력하세요.") LocalDate effectiveStartDate,
        @NotNull(message = "적용종료일을 입력하세요.") LocalDate effectiveEndDate,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}

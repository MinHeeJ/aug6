package kr.ac.knue.commonfoundation.basic34;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SaveEvaluationRuleSetRequest(
        @NotNull(message = "규정버전을 선택하세요.") Long ruleVersionId,
        @NotBlank(message = "적용 대상을 입력하세요.") String targetScope,
        @NotBlank(message = "기준·점수규칙명을 입력하세요.") String ruleSetName,
        @NotBlank(message = "규칙 상태를 선택하세요.") String ruleSetStatus,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotNull(message = "적용시작일을 입력하세요.") LocalDate effectiveStartDate,
        @NotNull(message = "적용종료일을 입력하세요.") LocalDate effectiveEndDate,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason
) {}

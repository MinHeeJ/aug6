package kr.ac.knue.commonfoundation.basic34;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveEvaluationScoreRequest(
        @NotNull(message = "규정버전을 선택하세요.") Long ruleVersionId,
        @NotNull(message = "관리항목을 선택하세요.") Long managementItemId,
        @NotBlank(message = "소속대학 코드를 입력하세요.") String organizationCode,
        @NotBlank(message = "평가연도를 입력하세요.") String evaluationYear,
        @NotNull(message = "평가점수를 입력하세요.") @PositiveOrZero(message = "평가점수는 0 이상이어야 합니다.") BigDecimal baseScore,
        @PositiveOrZero(message = "최대점수는 0 이상이어야 합니다.") BigDecimal maxScore,
        @NotNull(message = "적용시작일을 입력하세요.") LocalDate effectiveStartDate,
        @NotNull(message = "적용종료일을 입력하세요.") LocalDate effectiveEndDate,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}

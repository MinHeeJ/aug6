package kr.ac.knue.commonfoundation.basic34;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveParticipationRateRequest(
        @NotNull(message = "규정버전을 선택하세요.") Long ruleVersionId,
        @NotNull(message = "관리항목을 선택하세요.") Long managementItemId,
        @NotNull(message = "연구자 수를 입력하세요.") @Positive(message = "연구자 수는 1 이상이어야 합니다.") Integer researcherCount,
        @NotBlank(message = "참여구분을 입력하세요.") String participationType,
        @NotNull(message = "배분율을 입력하세요.") @PositiveOrZero(message = "배분율은 0 이상이어야 합니다.") BigDecimal distributionRate,
        @NotNull(message = "적용시작일을 입력하세요.") LocalDate effectiveStartDate,
        @NotNull(message = "적용종료일을 입력하세요.") LocalDate effectiveEndDate,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}

package kr.ac.knue.commonfoundation.basic59;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record SaveParticipationRateOperationSettingRequest(
        @NotNull(message = "규정버전을 선택하세요.") Long ruleVersionId,
        @NotBlank(message = "업적영역 코드를 입력하세요.") String achievementAreaCode,
        @NotBlank(message = "업적분류 코드를 입력하세요.") String achievementCategoryCode,
        String managementItemCode,
        @NotEmpty(message = "배분율 matrix를 입력하세요.") List<@Valid Rate> rates,
        String changeReason) {
    public record Rate(
            @NotBlank(message = "연구자 수 구간을 입력하세요.") String researcherCountBand,
            @NotBlank(message = "참여구분 코드를 입력하세요.") String participationTypeCode,
            @NotNull(message = "배분율을 입력하세요.")
            @DecimalMin(value = "0.00", message = "배분율은 0 이상이어야 합니다.")
            @DecimalMax(value = "100.00", message = "배분율은 100 이하이어야 합니다.") BigDecimal distributionRate) {
    }
}

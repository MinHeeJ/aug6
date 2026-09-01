package kr.ac.knue.commonfoundation.resultviewperiod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record SaveResultViewPeriodRequest(
        Long settingId,
        @NotBlank(message = "평가연도를 입력하세요.") String evaluationYear,
        @NotBlank(message = "소속대학 코드를 입력하세요.") String collegeOrganizationCode,
        String departmentOrganizationCode,
        @NotNull(message = "공개 시작일시를 입력하세요.") LocalDateTime viewStartAt,
        @NotNull(message = "공개 종료일시를 입력하세요.") LocalDateTime viewEndAt,
        @NotBlank(message = "공개 범위를 선택하세요.") String visibilityScope,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason
) {
}

package kr.ac.knue.commonfoundation.appealperiod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record SaveAppealPeriodRequest(
        Long settingId,
        @NotBlank(message = "평가연도를 입력하세요.") String evaluationYear,
        @NotBlank(message = "소속대학 코드를 입력하세요.") String collegeOrganizationCode,
        String departmentOrganizationCode,
        @NotNull(message = "이의신청 시작일시를 입력하세요.") LocalDateTime appealStartAt,
        @NotNull(message = "이의신청 종료일시를 입력하세요.") LocalDateTime appealEndAt,
        @NotNull(message = "처리 담당자를 입력하세요.") Long handlerUserId,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason
) {
}

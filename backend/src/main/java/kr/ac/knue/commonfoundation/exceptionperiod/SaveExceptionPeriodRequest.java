package kr.ac.knue.commonfoundation.exceptionperiod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record SaveExceptionPeriodRequest(
        Long settingId,
        @NotBlank(message = "평가연도를 입력하세요.") String evaluationYear,
        @NotNull(message = "대상 교원을 입력하세요.") Long teacherUserId,
        @NotBlank(message = "평가영역 코드를 입력하세요.") String areaCode,
        @NotBlank(message = "대상 기능 코드를 입력하세요.") String targetFunctionCode,
        @NotNull(message = "예외 시작일시를 입력하세요.") LocalDateTime exceptionStartAt,
        @NotNull(message = "예외 종료일시를 입력하세요.") LocalDateTime exceptionEndAt,
        @NotBlank(message = "승인사유를 입력하세요.") String approvalReason,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason
) {
}

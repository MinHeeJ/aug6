package kr.ac.knue.commonfoundation.temporarypermissions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record TemporaryPermissionCreateRequest(
        @NotNull(message = "대상 교원 ID를 입력하세요.") Long userId,
        @NotBlank(message = "업무자료 식별자를 입력하세요.") String workDataRef,
        @NotBlank(message = "기능구분을 선택하세요.") String functionType,
        @NotNull(message = "유효 시작일시를 입력하세요.") LocalDateTime validStartAt,
        @NotNull(message = "유효 종료일시를 입력하세요.") LocalDateTime validEndAt,
        @NotBlank(message = "변경 사유를 입력하세요.") @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason) {
}

package kr.ac.knue.commonfoundation.periodpermissions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record PeriodPermissionSaveRequest(
        @NotBlank(message = "업무기간 ID를 입력하세요.") String businessPeriodId,
        @NotNull(message = "기능 권한 ID를 선택하세요.") Long functionPermissionId,
        @NotNull(message = "시작일시를 입력하세요.") LocalDateTime effectiveStartAt,
        LocalDateTime effectiveEndAt,
        @NotBlank(message = "변경 사유를 입력하세요.") @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason) {
}

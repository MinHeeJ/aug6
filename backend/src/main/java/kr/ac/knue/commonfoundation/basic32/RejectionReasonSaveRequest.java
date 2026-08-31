package kr.ac.knue.commonfoundation.basic32;

import jakarta.validation.constraints.NotBlank;

public record RejectionReasonSaveRequest(
        @NotBlank(message = "업무유형을 선택하세요.") String businessType,
        @NotBlank(message = "반려사유 코드를 입력하세요.") String reasonCode,
        @NotBlank(message = "표준 문구를 입력하세요.") String standardMessage,
        @NotBlank(message = "추가 의견 허용 여부를 선택하세요.") String additionalOpinionAllowedYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}

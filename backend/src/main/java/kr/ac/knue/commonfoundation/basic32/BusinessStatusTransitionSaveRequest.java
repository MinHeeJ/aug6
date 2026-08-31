package kr.ac.knue.commonfoundation.basic32;

import jakarta.validation.constraints.NotBlank;

public record BusinessStatusTransitionSaveRequest(
        @NotBlank(message = "상태정의 버전을 입력하세요.") String definitionVersion,
        @NotBlank(message = "업무유형을 선택하세요.") String businessType,
        @NotBlank(message = "현재 상태코드를 입력하세요.") String fromStatusCode,
        @NotBlank(message = "다음 상태코드를 입력하세요.") String toStatusCode,
        @NotBlank(message = "실행 역할을 선택하세요.") String executorRoleCode,
        @NotBlank(message = "필수의견 여부를 선택하세요.") String opinionRequiredYn,
        @NotBlank(message = "필수첨부 여부를 선택하세요.") String attachmentRequiredYn,
        @NotBlank(message = "취소 가능 여부를 선택하세요.") String cancellableYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}

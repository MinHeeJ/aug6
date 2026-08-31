package kr.ac.knue.commonfoundation.basic32;

import jakarta.validation.constraints.NotBlank;

public record BusinessStatusCodeSaveRequest(
        @NotBlank(message = "상태정의 버전을 입력하세요.") String definitionVersion,
        @NotBlank(message = "업무유형을 선택하세요.") String businessType,
        @NotBlank(message = "상태코드를 입력하세요.") String statusCode,
        @NotBlank(message = "상태 표시명을 입력하세요.") String displayName,
        @NotBlank(message = "사용여부를 선택하세요.") String systemUseYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}

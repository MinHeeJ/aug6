package kr.ac.knue.commonfoundation.evaluationyears;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EvaluationYearSettingsRequest(
        @NotNull(message = "현재 기준연도를 입력하세요.") Integer currentEvaluationYear,
        @NotNull(message = "기본 조회연도를 입력하세요.") Integer defaultSearchYear,
        @NotBlank(message = "변경 사유를 입력하세요.") @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason,
        List<@Valid Preparation> preparations) {
    public record Preparation(
            @NotNull(message = "대상연도를 입력하세요.") Integer targetYear,
            @NotBlank(message = "복사 여부를 선택하세요.") String copyRequestedYn,
            @NotBlank(message = "초기화 여부를 선택하세요.") String resetRequestedYn,
            @NotBlank(message = "대상연도 준비 상태 변경 사유를 입력하세요.") @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason) {
    }
}

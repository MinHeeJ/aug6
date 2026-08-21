package kr.ac.knue.commonfoundation.operations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record BaseYearSaveRequest(@NotNull Integer baseYear, @NotNull Integer currentEvaluationYear,
        @NotNull Integer defaultSearchYear,
        @NotBlank @Pattern(regexp = "Y|N", message = "복사 여부는 Y 또는 N이어야 합니다.") String copyRequestedYn,
        @NotBlank @Pattern(regexp = "Y|N", message = "초기화 여부는 Y 또는 N이어야 합니다.") String initializeRequestedYn,
        @NotBlank String changeReason) {
}

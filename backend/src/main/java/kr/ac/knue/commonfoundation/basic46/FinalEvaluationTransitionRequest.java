package kr.ac.knue.commonfoundation.basic46;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FinalEvaluationTransitionRequest(
        @NotBlank(message = "처리구분을 입력하세요.")
        String actionType,
        @Pattern(regexp = "^$|^[0-9]{4}$", message = "평가연도는 4자리 연도여야 합니다.")
        String evaluationYear,
        String cancelReason,
        String reason) {
}

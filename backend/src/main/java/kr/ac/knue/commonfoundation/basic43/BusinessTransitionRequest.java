package kr.ac.knue.commonfoundation.basic43;

import jakarta.validation.constraints.NotBlank;

public record BusinessTransitionRequest(
        @NotBlank(message = "처리구분을 선택하세요.") String actionType,
        String reasonCode,
        String opinion,
        String evidenceRef) {
}

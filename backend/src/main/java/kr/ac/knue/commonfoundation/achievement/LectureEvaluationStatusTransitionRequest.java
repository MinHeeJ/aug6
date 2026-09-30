package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.constraints.NotBlank;

/**
 * Requests a supported lecture-evaluation certification-status transition.
 */
public record LectureEvaluationStatusTransitionRequest(
        @NotBlank(message = "다음 인증상태를 선택하세요.") String nextStatus,
        @NotBlank(message = "처리구분을 선택하세요.") String actionType,
        String reasonCode,
        String opinion) {
}

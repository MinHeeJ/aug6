package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.constraints.NotBlank;

/**
 * Explicit certification-state action for a persisted lecture-evaluation achievement.
 */
public record LectureEvaluationAchievementTransitionRequest(
        @NotBlank(message = "처리구분을 선택하세요.") String actionType,
        String reasonCode,
        String opinion,
        String changeReason
) {
}

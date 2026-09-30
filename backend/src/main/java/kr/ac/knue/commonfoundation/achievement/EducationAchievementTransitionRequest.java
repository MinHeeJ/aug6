package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.constraints.NotBlank;

/**
 * Requests a domain-owned education-achievement certification-status transition.
 */
public record EducationAchievementTransitionRequest(
        @NotBlank(message = "다음 인증상태를 선택하세요.") String nextStatus,
        String reasonCode,
        String opinion) {
}

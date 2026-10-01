package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.constraints.NotBlank;

/** Request for an allowed lecture-achievement certification status transition. */
public record LectureAchievementTransitionRequest(
        @NotBlank(message = "처리구분을 선택하세요.") String actionType,
        String reasonCode,
        String opinion,
        String changeReason
) {
}

package kr.ac.knue.commonfoundation.educationachievement;

import jakarta.validation.constraints.NotBlank;
import org.apache.ibatis.type.Alias;

/** Carries an education-achievement workflow action and an optional rejection or submission opinion. */
@Alias("EducationAchievementBusinessTransitionRequest")
public record BusinessTransitionRequest(
        @NotBlank(message = "처리구분을 선택하세요.") String actionType,
        String reasonCode,
        String opinion) {
}

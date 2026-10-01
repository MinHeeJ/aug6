package kr.ac.knue.commonfoundation.basic81;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Defines the atomic create or update command for a degree-completion header and its students. */
public record SaveDegreeCompletionAchievementRequest(
        Long achievementId,
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        String attachmentRef,
        @NotEmpty(message = "지도학생을 1명 이상 입력하세요.") List<DegreeCompletionStudentRequest> students) {
}

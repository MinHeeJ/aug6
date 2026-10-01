package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Request boundary for atomically saving a degree-completion header and its student details. */
public record SaveDegreeCompletionAchievementRequest(
        Long achievementId,
        Long targetUserId,
        String evaluationYear,
        String organizationCode,
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate occurredDate,
        String achievementDetail,
        String attachmentRef,
        @NotEmpty(message = "지도학생을 1명 이상 입력하세요.") List<@Valid DegreeCompletionStudentRequest> students,
        String changeReason
) {
}

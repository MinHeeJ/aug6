package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Request boundary for atomically saving a student-guidance header and its student details. */
public record SaveStudentGuidanceAchievementRequest(
        Long achievementId,
        Long targetUserId,
        String evaluationYear,
        String organizationCode,
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "지도시작일을 입력하세요.") LocalDate guidanceStartDate,
        @NotNull(message = "지도종료일을 입력하세요.") LocalDate guidanceEndDate,
        String attachmentRef,
        @NotEmpty(message = "지도학생을 1명 이상 입력하세요.") List<@Valid StudentGuidanceStudentRequest> students,
        String changeReason
) {
}

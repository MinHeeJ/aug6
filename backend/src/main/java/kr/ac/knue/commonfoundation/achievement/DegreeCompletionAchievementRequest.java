package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

/** Binds a degree-completion header and its required guided-student details for one atomic save. */
public record DegreeCompletionAchievementRequest(
        Long achievementId,
        @NotBlank(message = "관리항목 코드를 입력하세요.") String managementItemCode,
        LocalDate occurredDate,
        JsonNode achievementDetail,
        EducationAchievementStatus nextStatus,
        String transitionReason,
        Integer attachmentCount,
        String changeReason,
        @NotEmpty(message = "지도학생을 한 명 이상 입력하세요.") List<@Valid DegreeCompletionStudentRequest> students) {
}

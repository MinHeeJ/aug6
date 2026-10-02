package kr.ac.knue.commonfoundation.basic81;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Defines an atomic student-guidance header plus repeated student-detail command. */
public record SaveStudentGuidanceAchievementRequest(
        Long achievementId,
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "지도 시작일을 입력하세요.") LocalDate guidanceStartDate,
        @NotNull(message = "지도 종료일을 입력하세요.") LocalDate guidanceEndDate,
        String attachmentRef,
        @NotEmpty(message = "지도학생을 1명 이상 입력하세요.") List<JsonNode> students) {
}

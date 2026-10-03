package kr.ac.knue.commonfoundation.lectureimprovement;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Defines the OpenAPI command payload for a lecture-improvement achievement. */
public record LectureImprovementRequest(
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        @NotBlank(message = "강의개선 내용을 입력하세요.") String achievementContent,
        @NotNull(message = "학년도를 입력하세요.") @Min(value = 2000, message = "학년도는 2000 이상이어야 합니다.")
                Integer academicYear,
        @NotNull(message = "학기를 입력하세요.") Integer semester,
        List<String> attachmentIds) {
}

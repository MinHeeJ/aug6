package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Request payload for creating or replacing a course-operation achievement. */
public record CourseOperationRequest(
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        @NotBlank(message = "실적내역을 입력하세요.") String performanceDetails,
        List<String> attachmentIds) {
}

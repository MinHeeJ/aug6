package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Input payload for creating or replacing one employment-rate improvement achievement. */
public record EmploymentRateImprovementRequest(
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        String mockExamQuestionPeriod,
        List<String> attachmentIds) {
}

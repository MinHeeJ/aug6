package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Client-editable fields only; ownership, year and lifecycle remain server-controlled. */
public record EmploymentRateImprovementRequest(
        @NotBlank(message = "관리항목을 선택하세요.") @Size(max = 50) String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        String mockExamQuestionPeriod,
        List<String> attachmentIds) {
}

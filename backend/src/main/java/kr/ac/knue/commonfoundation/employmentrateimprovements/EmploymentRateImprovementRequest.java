package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** Creation/update fields; the path alone selects an existing achievement. */
public record EmploymentRateImprovementRequest(
        @NotBlank(message = "관리항목을 입력하세요.") @Size(max = 50) String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        @Size(max = 300) String mockExamQuestionPeriod,
        List<@NotBlank @Size(max = 200) String> attachmentIds,
        @Pattern(regexp = "[0-9]{4}") String evaluationYear,
        @Size(max = 300) String achievementName,
        JsonNode achievementDetail,
        @Size(max = 300) String attachmentRef) {
}

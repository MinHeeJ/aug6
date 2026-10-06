package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Approved mutable fields only; ownership, identity and lifecycle are server controlled. */
public record EmploymentRateImprovementRequest(
        @NotBlank(message = "관리항목을 입력하세요.") @Size(max = 50) String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        @Size(max = 500) String mockExamQuestionPeriod,
        List<@NotBlank String> attachmentIds) {
    /** Rejects forged ownership/status fields instead of silently accepting them. */
    @JsonAnySetter
    public void rejectUnknownField(String field, Object value) {
        throw new IllegalArgumentException("허용되지 않은 입력 필드입니다: " + field);
    }
}

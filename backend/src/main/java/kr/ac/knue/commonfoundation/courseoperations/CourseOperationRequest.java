package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Editable FR-030 fields only; identity, ownership and lifecycle are server-controlled. */
public record CourseOperationRequest(
        @NotBlank(message = "관리항목을 입력하세요.")
        @Size(max = 50) String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        @NotBlank(message = "실적내역을 입력하세요.") String performanceDetails,
        List<String> attachmentIds) {
    /** Reject unknown fields instead of silently accepting a status or owner injection. */
    @JsonAnySetter
    public void rejectUnknownField(String name, JsonNode value) {
        throw new IllegalArgumentException("허용되지 않은 입력 항목입니다.");
    }
}

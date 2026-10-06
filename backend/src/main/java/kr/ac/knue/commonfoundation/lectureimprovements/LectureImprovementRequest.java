package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Only editable wire fields; identity, ownership and status are always server-owned. */
public record LectureImprovementRequest(
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @NotBlank String achievementContent,
        @NotNull @Min(2000) @Max(9999) Integer academicYear,
        @NotNull @Min(1) @Max(2) Integer semester,
        List<String> attachmentIds) {
    /** Rejects unknown fields rather than silently accepting status/owner injection. */
    @JsonAnySetter
    public void rejectUnknown(String field, JsonNode value) {
        throw new IllegalArgumentException("지원하지 않는 입력 필드입니다.");
    }
}

package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;

/** Editable FR-032 fields only; identity, ownership and lifecycle remain server controlled. */
public record EmploymentRateAchievementRequest(
        @NotBlank(message = "관리항목을 입력하세요.") @Size(max = 50) String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        @Size(max = 500) String achievementName,
        List<@NotBlank String> attachmentIds) {
    /** Rejects source-controlled fields rather than silently accepting owner/status injection. */
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new BusinessValidationException("수정할 수 없는 입력항목입니다.",
                List.of(new ValidationError(name, "승인된 입력항목만 전송하세요.")));
    }
}

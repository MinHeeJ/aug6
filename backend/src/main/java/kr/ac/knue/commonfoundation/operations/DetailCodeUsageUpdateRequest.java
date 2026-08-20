package kr.ac.knue.commonfoundation.operations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record DetailCodeUsageUpdateRequest(
        @NotBlank @Pattern(regexp = "Y|N", message = "사용여부는 Y 또는 N이어야 합니다.") String systemUseYn,
        LocalDate validStartDate,
        LocalDate validEndDate,
        @NotBlank String changeReason) {
}

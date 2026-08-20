package kr.ac.knue.commonfoundation.codes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record DetailCodeUsageSettingsRequest(
        @NotEmpty(message = "저장할 코드 사용 설정을 선택하세요.") List<@Valid Item> items) {
    public record Item(
            @NotBlank(message = "코드값을 선택하세요.") String codeValue,
            String codeName,
            @NotBlank(message = "사용여부를 선택하세요.") String systemUseYn,
            LocalDate validStartDate,
            LocalDate validEndDate,
            @NotBlank(message = "변경 사유를 입력하세요.") @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason) {
    }
}

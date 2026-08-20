package kr.ac.knue.commonfoundation.settings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CommonSystemSettingsRequest(
        @NotEmpty(message = "저장할 공통 환경설정을 선택하세요.") List<@Valid Item> settings) {
    public record Item(
            @NotBlank(message = "설정 항목을 선택하세요.") String settingKey,
            @NotBlank(message = "설정값을 입력하세요.") String settingValue,
            @NotBlank(message = "단위를 입력하세요.") String unit,
            Long userId,
            @NotBlank(message = "변경 사유를 입력하세요.") @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason) {
    }
}

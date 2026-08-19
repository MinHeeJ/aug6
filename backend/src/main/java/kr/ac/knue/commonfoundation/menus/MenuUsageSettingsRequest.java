package kr.ac.knue.commonfoundation.menus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record MenuUsageSettingsRequest(
        @NotEmpty(message = "저장할 메뉴 사용 설정을 선택하세요.") List<@Valid Item> items) {
    public record Item(
            @NotNull(message = "메뉴를 선택하세요.") Long menuId,
            @NotBlank(message = "사용여부를 선택하세요.") String systemUseYn,
            @NotNull(message = "노출 시작일시를 입력하세요.") LocalDateTime exposureStartAt,
            LocalDateTime exposureEndAt,
            @NotBlank(message = "변경 사유를 입력하세요.") @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason) {
    }
}

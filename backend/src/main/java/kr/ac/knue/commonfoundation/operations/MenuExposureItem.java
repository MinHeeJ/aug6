package kr.ac.knue.commonfoundation.operations;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public record MenuExposureItem(@NotNull Long menuId,
        @NotNull @Pattern(regexp = "Y|N", message = "사용여부는 Y 또는 N이어야 합니다.") String systemUseYn,
        LocalDateTime exposureStartAt, LocalDateTime exposureEndAt) {
}

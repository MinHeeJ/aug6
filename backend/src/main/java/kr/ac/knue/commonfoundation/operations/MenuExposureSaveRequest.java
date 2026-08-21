package kr.ac.knue.commonfoundation.operations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MenuExposureSaveRequest(@Valid @NotEmpty List<MenuExposureItem> settings, @NotBlank String changeReason) {
}

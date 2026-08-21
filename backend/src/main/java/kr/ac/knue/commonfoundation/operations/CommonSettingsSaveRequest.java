package kr.ac.knue.commonfoundation.operations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CommonSettingsSaveRequest(@Valid @NotEmpty List<CommonSettingInput> settings, @NotBlank String changeReason) {
}

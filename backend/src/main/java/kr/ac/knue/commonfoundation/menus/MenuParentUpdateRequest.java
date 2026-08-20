package kr.ac.knue.commonfoundation.menus;

import jakarta.validation.constraints.NotBlank;

public record MenuParentUpdateRequest(
        Long parentMenuId,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}

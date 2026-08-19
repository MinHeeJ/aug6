package kr.ac.knue.commonfoundation.menus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuParentUpdateRequest(
        @NotNull(message = "부모 메뉴를 선택하세요.") Long parentMenuId,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}

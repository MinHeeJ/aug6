package kr.ac.knue.commonfoundation.menus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MenuExecutionRequest(
        @NotBlank(message = "메뉴명을 입력하세요.") @Size(max = 200, message = "메뉴명은 200자 이하여야 합니다.") String menuName,
        @NotBlank(message = "화면ID를 입력하세요.") @Size(max = 100, message = "화면ID는 100자 이하여야 합니다.") String screenId,
        @NotBlank(message = "URL을 입력하세요.") @Size(max = 300, message = "URL은 300자 이하여야 합니다.") String url,
        @Size(max = 100, message = "아이콘은 100자 이하여야 합니다.") String icon,
        @Size(max = 100, message = "업무구분은 100자 이하여야 합니다.") String businessCategory,
        @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.") String description,
        @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.") String changeReason) {
}

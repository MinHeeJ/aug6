package kr.ac.knue.commonfoundation.menus;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MenuReorderRequest(
        Long parentMenuId,
        @NotEmpty(message = "정렬할 메뉴 목록을 입력하세요.") List<Long> orderedMenuIds,
        String changeReason) {
}

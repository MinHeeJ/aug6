package kr.ac.knue.commonfoundation.menus;

import java.time.LocalDateTime;

public record MenuExecutionRow(
        Long menuId,
        Long parentMenuId,
        String menuType,
        String menuName,
        String screenId,
        String url,
        String icon,
        String businessCategory,
        String description,
        String status,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}

package kr.ac.knue.commonfoundation.menus;

import java.time.LocalDateTime;

public record MenuTreeRow(
        Long menuId,
        Long parentMenuId,
        String menuType,
        String menuName,
        Integer displayOrder,
        String screenId,
        String url,
        String icon,
        String businessCategory,
        String description,
        String systemUseYn,
        String status,
        String changeReason,
        LocalDateTime updatedAt) {
}

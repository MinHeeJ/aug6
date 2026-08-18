package kr.ac.knue.commonfoundation.menus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record MenuTreeNode(
        Long menuId,
        Long parentMenuId,
        String menuType,
        String menuName,
        int displayOrder,
        String screenId,
        String url,
        String icon,
        String businessCategory,
        String description,
        String systemUseYn,
        String status,
        String changeReason,
        LocalDateTime updatedAt,
        List<MenuTreeNode> children) {
    public static MenuTreeNode from(MenuTreeRow row) {
        return new MenuTreeNode(
                row.menuId(),
                row.parentMenuId(),
                row.menuType(),
                row.menuName(),
                row.displayOrder(),
                row.screenId(),
                row.url(),
                row.icon(),
                row.businessCategory(),
                row.description(),
                row.systemUseYn(),
                row.status(),
                row.changeReason(),
                row.updatedAt(),
                new ArrayList<>());
    }
}

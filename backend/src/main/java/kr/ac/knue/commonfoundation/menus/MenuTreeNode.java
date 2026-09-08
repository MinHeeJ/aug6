package kr.ac.knue.commonfoundation.menus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record MenuTreeNode(
        Long menuId,
        Long parentMenuId,
        String menuType,
        String menuName,
        String menuNameEn,
        String displayName,
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
    public MenuTreeNode(Long menuId,
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
        this(menuId, parentMenuId, menuType, menuName, null, menuName, displayOrder, screenId, url, icon,
                businessCategory, description, systemUseYn, status, changeReason, updatedAt, children);
    }

    public static MenuTreeNode from(MenuTreeRow row) {
        return from(row, "ko");
    }

    public static MenuTreeNode from(MenuTreeRow row, String lang) {
        String displayName = resolveDisplayName(row.menuName(), row.menuNameEn(), lang);
        return new MenuTreeNode(
                row.menuId(),
                row.parentMenuId(),
                row.menuType(),
                row.menuName(),
                row.menuNameEn(),
                displayName,
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

    private static String resolveDisplayName(String menuName, String menuNameEn, String lang) {
        if ("en".equals(lang) && menuNameEn != null && !menuNameEn.isBlank()) {
            return menuNameEn;
        }
        return menuName;
    }
}

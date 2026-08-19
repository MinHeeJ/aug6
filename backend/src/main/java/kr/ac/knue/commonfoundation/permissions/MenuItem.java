package kr.ac.knue.commonfoundation.permissions;

import java.util.ArrayList;
import java.util.List;

public record MenuItem(Long menuId, Long parentMenuId, String menuName, String screenId, String url, String icon,
                       int displayOrder, List<MenuItem> children) {
    public MenuItem withChildren(List<MenuItem> nextChildren) {
        return new MenuItem(menuId, parentMenuId, menuName, screenId, url, icon, displayOrder, nextChildren);
    }

    public static MenuItem leaf(MenuRow row) {
        return new MenuItem(row.menuId(), row.parentMenuId(), row.menuName(), row.screenId(), row.url(), row.icon(), row.displayOrder(), new ArrayList<>());
    }
}

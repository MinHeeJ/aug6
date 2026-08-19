package kr.ac.knue.commonfoundation.permissions;

public record MenuRow(Long menuId, Long parentMenuId, String menuName, String screenId, String url, String icon, int displayOrder) {
}

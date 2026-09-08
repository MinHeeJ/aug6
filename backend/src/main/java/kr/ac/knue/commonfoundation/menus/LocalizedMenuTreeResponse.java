package kr.ac.knue.commonfoundation.menus;

import java.util.List;

public record LocalizedMenuTreeResponse(String lang, List<MenuTreeNode> rows) {
}

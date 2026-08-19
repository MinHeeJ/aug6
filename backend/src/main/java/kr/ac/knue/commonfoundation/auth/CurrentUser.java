package kr.ac.knue.commonfoundation.auth;

import java.util.List;
import kr.ac.knue.commonfoundation.permissions.MenuItem;

public record CurrentUser(Long userId, String loginId, String employeeNo, String name, List<String> roles, List<MenuItem> menus) {
}

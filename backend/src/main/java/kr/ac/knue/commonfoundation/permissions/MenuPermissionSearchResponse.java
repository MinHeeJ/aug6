package kr.ac.knue.commonfoundation.permissions;

import java.util.List;

public record MenuPermissionSearchResponse(List<MenuPermissionRow> permissions, int page, int size, int totalElements) {
}

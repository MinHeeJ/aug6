package kr.ac.knue.commonfoundation.menus;

import java.util.List;

public record MenuUsageSearchResponse(List<MenuUsageRow> settings, int page, int size, int totalElements) {
}

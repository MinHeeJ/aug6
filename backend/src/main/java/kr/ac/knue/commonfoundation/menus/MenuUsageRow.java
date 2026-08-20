package kr.ac.knue.commonfoundation.menus;

import java.time.LocalDateTime;

public record MenuUsageRow(
        Long menuId,
        Long parentMenuId,
        String topMenuName,
        String middleMenuName,
        String menuName,
        String screenId,
        String url,
        String systemUseYn,
        LocalDateTime exposureStartAt,
        LocalDateTime exposureEndAt,
        String status,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}

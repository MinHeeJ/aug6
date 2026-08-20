package kr.ac.knue.commonfoundation.operations;

import java.time.LocalDateTime;

public record MenuExposureSetting(Long menuId, String menuName, String systemUseYn, LocalDateTime exposureStartAt, LocalDateTime exposureEndAt, String changeReason, LocalDateTime updatedAt) {
}

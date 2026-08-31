package kr.ac.knue.commonfoundation.basic33;

import java.time.LocalDateTime;

public record EvaluationItemRow(
        Long itemId,
        Long areaId,
        Long ruleVersionId,
        String versionCode,
        String versionStatus,
        String areaCode,
        String areaName,
        String itemCode,
        String itemName,
        String parentItemCode,
        Integer sortOrder,
        String activeYn,
        String scoreApplyMethod,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}

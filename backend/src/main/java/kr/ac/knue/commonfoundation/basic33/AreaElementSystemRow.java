package kr.ac.knue.commonfoundation.basic33;

import java.time.LocalDateTime;

public record AreaElementSystemRow(
        Long systemSettingId,
        Long elementId,
        Long itemId,
        Long areaId,
        Long ruleVersionId,
        String versionCode,
        String versionStatus,
        String areaCode,
        String areaName,
        String itemCode,
        String itemName,
        String evaluationYear,
        String elementCode,
        String elementName,
        String targetScope,
        String activeYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}

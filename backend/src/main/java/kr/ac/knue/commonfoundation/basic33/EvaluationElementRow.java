package kr.ac.knue.commonfoundation.basic33;

import java.time.LocalDateTime;

public record EvaluationElementRow(
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
        Integer sortOrder,
        String activeYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}

package kr.ac.knue.commonfoundation.basic33;

import java.time.LocalDateTime;

public record EvaluationManagementItemRow(
        Long managementItemId,
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
        String managementItemCode,
        String managementItemName,
        Integer sortOrder,
        String activeYn,
        String teacherEditableYn,
        String requiredYn,
        String dataType,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}

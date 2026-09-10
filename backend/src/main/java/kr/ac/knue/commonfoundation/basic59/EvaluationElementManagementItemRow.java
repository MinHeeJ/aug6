package kr.ac.knue.commonfoundation.basic59;

import java.time.LocalDateTime;

public record EvaluationElementManagementItemRow(
        Long settingId,
        Long ruleVersionId,
        String versionCode,
        String versionStatus,
        String evaluationYear,
        String areaCode,
        String elementCode,
        String managementItemCode,
        String managementItemName,
        String teacherEditablePart,
        Integer sortOrder,
        String activeYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}

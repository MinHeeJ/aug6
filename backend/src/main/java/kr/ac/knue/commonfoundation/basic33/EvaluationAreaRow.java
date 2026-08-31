package kr.ac.knue.commonfoundation.basic33;

import java.time.LocalDateTime;

public record EvaluationAreaRow(
        Long areaId,
        Long ruleVersionId,
        String versionCode,
        String versionStatus,
        String areaCode,
        String areaName,
        Integer sortOrder,
        String activeYn,
        String periodApplyMethod,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}

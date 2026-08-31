package kr.ac.knue.commonfoundation.basic34;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EvaluationRuleSetRow(
        Long ruleSetId,
        Long ruleVersionId,
        String versionCode,
        String versionStatus,
        String targetScope,
        String ruleSetName,
        String ruleSetStatus,
        String activeYn,
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt
) {}

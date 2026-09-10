package kr.ac.knue.commonfoundation.basic60;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SaveEvaluationElementManagementItemSettingRequest(
        @NotNull Long ruleVersionId,
        @NotBlank String targetScope,
        @NotBlank String areaCode,
        @NotBlank String itemCode,
        @NotBlank String evaluationYear,
        @NotBlank String elementCode,
        @NotBlank String managementItemCode,
        @NotBlank String managementItemName,
        @NotNull Integer sortOrder,
        @NotBlank String activeYn,
        @NotBlank String teacherEditableYn,
        @NotNull LocalDate effectiveStartDate,
        @NotNull LocalDate effectiveEndDate,
        @NotBlank String changeReason) {}

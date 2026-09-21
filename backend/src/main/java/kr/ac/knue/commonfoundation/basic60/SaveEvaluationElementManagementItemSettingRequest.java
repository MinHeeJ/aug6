package kr.ac.knue.commonfoundation.basic60;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Captures the editable operating values for an evaluation-element management item.
 * The legacy flag name remains an input alias while clients migrate to the more precise part field.
 */
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
        @NotBlank @JsonAlias("teacherEditableYn") String teacherEditablePart,
        @NotNull LocalDate effectiveStartDate,
        @NotNull LocalDate effectiveEndDate,
        @NotBlank String changeReason) {}

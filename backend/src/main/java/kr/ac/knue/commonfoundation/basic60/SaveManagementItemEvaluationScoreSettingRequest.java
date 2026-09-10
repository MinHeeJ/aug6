package kr.ac.knue.commonfoundation.basic60;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveManagementItemEvaluationScoreSettingRequest(
        @NotNull Long ruleVersionId,
        @NotBlank String targetScope,
        @NotBlank String areaCode,
        @NotBlank String itemCode,
        @NotBlank String evaluationYear,
        @NotBlank String elementCode,
        @NotBlank String managementItemCode,
        @NotBlank String organizationCode,
        String organizationName,
        @NotNull BigDecimal evaluationScore,
        BigDecimal maxScore,
        @NotNull Integer sortOrder,
        @NotBlank String activeYn,
        @NotNull LocalDate effectiveStartDate,
        @NotNull LocalDate effectiveEndDate,
        @NotBlank String changeReason) {}

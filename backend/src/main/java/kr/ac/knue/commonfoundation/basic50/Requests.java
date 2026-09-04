package kr.ac.knue.commonfoundation.basic50;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

record BusinessSettingSaveRequest(
        Long settingId,
        @NotBlank String evaluationYear,
        @NotBlank String organizationCode,
        String evaluationUnitCode,
        @NotNull LocalDate effectiveStartDate,
        @NotNull LocalDate effectiveEndDate,
        @NotNull Long managerUserId,
        @NotBlank String targetScope,
        @NotBlank String activeYn,
        @NotBlank String changeReason) {
}

record CollegeEvaluationUnitAuthoritySaveRequest(
        Long authorityId,
        @NotBlank String evaluationYear,
        @NotBlank String organizationCode,
        @NotBlank String evaluationUnitCode,
        @NotNull Long managerUserId,
        @NotBlank String inputAllowedYn,
        @NotBlank String outputAllowedYn,
        @NotBlank String modifyAllowedYn,
        Long teacherUserId,
        @NotNull LocalDate effectiveStartDate,
        @NotNull LocalDate effectiveEndDate,
        @NotBlank String activeYn,
        @NotBlank String changeReason) {
}

record ResearchCriterionSaveRequest(
        Long criterionId,
        @NotBlank String areaCode,
        @NotBlank String areaName,
        @NotBlank String managementCriterionCode,
        @NotBlank String managementCriterionName,
        String parentCriterionCode,
        @NotBlank String activeYn,
        @NotBlank String changeReason) {
}

record ResearchAchievementConfirmationRequest(@NotBlank String managementCriterionCode, @NotBlank String changeReason) {
}

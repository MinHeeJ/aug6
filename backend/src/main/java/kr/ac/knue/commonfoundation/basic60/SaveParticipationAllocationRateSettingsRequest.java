package kr.ac.knue.commonfoundation.basic60;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Batch-save envelope for the participation allocation-rate matrix.
 *
 * <p>Keeping the common rule-version and audit reason at the envelope level prevents rows in a
 * single user action from being written under different regulatory contexts.</p>
 */
public record SaveParticipationAllocationRateSettingsRequest(
        @NotNull Long ruleVersionId,
        @NotBlank String targetScope,
        @NotEmpty List<@Valid SaveParticipationAllocationRateSettingItem> items,
        @NotBlank String changeReason) {

    /** A single editable cell row supplied by the allocation-rate matrix. */
    public record SaveParticipationAllocationRateSettingItem(
            @NotBlank String areaCode,
            @NotBlank String itemCode,
            @NotBlank String evaluationYear,
            @NotBlank String elementCode,
            @NotBlank String managementItemCode,
            @NotNull Integer researcherCount,
            @NotBlank String participationType,
            @NotNull java.math.BigDecimal allocationRate,
            @NotBlank String activeYn,
            @NotNull java.time.LocalDate effectiveStartDate,
            @NotNull java.time.LocalDate effectiveEndDate) {

        SaveParticipationAllocationRateSettingRequest toSaveRequest(
                Long ruleVersionId, String targetScope, String changeReason) {
            return new SaveParticipationAllocationRateSettingRequest(
                    ruleVersionId, targetScope, areaCode, itemCode, evaluationYear, elementCode,
                    managementItemCode, researcherCount, participationType, allocationRate, activeYn,
                    effectiveStartDate, effectiveEndDate, changeReason);
        }
    }
}

package kr.ac.knue.commonfoundation.basic60;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Captures the matrix-level participation allocation update so all submitted rows share one
 * rule-version, scope, reason, authorization check, and transaction boundary.
 */
public record SaveParticipationAllocationRateSettingsBatchRequest(
        @NotNull Long ruleVersionId,
        @NotBlank String targetScope,
        @NotBlank String changeReason,
        @NotEmpty List<@Valid Item> items) {

    /** Represents one editable researcher-count and participation-type cell in the matrix. */
    public record Item(
            @NotBlank String areaCode,
            @NotBlank String itemCode,
            @NotBlank String evaluationYear,
            @NotBlank String elementCode,
            @NotBlank String managementItemCode,
            @NotNull Integer researcherCount,
            @NotBlank String participationType,
            @NotNull BigDecimal allocationRate,
            @NotBlank String activeYn,
            @NotNull LocalDate effectiveStartDate,
            @NotNull LocalDate effectiveEndDate) {

        /** Converts the matrix cell to the established per-row persistence command. */
        SaveParticipationAllocationRateSettingRequest toSaveRequest(
                Long ruleVersionId, String targetScope, String changeReason) {
            return new SaveParticipationAllocationRateSettingRequest(
                    ruleVersionId, targetScope, areaCode, itemCode, evaluationYear, elementCode,
                    managementItemCode, researcherCount, participationType, allocationRate, activeYn,
                    effectiveStartDate, effectiveEndDate, changeReason);
        }
    }
}

package kr.ac.knue.commonfoundation.basic60;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a persisted operational setting row shared by the BASIC-60 read and save endpoints.
 */
public record OperationalSettingRow(
        Long settingId,
        Long ruleVersionId,
        String versionCode,
        String versionStatus,
        String targetScope,
        String areaCode,
        String itemCode,
        String evaluationYear,
        String elementCode,
        Long managementItemId,
        String managementItemCode,
        String managementItemName,
        String organizationCode,
        String organizationName,
        Integer researcherCount,
        String participationType,
        BigDecimal allocationRate,
        BigDecimal evaluationScore,
        BigDecimal maxScore,
        Integer sortOrder,
        String activeYn,
        String teacherEditableYn,
        String teacherEditablePart,
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate,
        String evaluationConfirmedYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
    /**
     * Retains the BASIC-60 constructor shape for callers that only have the legacy flag.
     */
    public OperationalSettingRow(
            Long settingId, Long ruleVersionId, String versionCode, String versionStatus,
            String targetScope, String areaCode, String itemCode, String evaluationYear,
            String elementCode, Long managementItemId, String managementItemCode,
            String managementItemName, String organizationCode, String organizationName,
            Integer researcherCount, String participationType, BigDecimal allocationRate,
            BigDecimal evaluationScore, BigDecimal maxScore, Integer sortOrder, String activeYn,
            String teacherEditableYn, LocalDate effectiveStartDate, LocalDate effectiveEndDate,
            String evaluationConfirmedYn, String changeReason, Long updatedBy, LocalDateTime updatedAt) {
        this(settingId, ruleVersionId, versionCode, versionStatus, targetScope, areaCode, itemCode,
                evaluationYear, elementCode, managementItemId, managementItemCode, managementItemName,
                organizationCode, organizationName, researcherCount, participationType, allocationRate,
                evaluationScore, maxScore, sortOrder, activeYn, teacherEditableYn, teacherEditableYn,
                effectiveStartDate, effectiveEndDate, evaluationConfirmedYn, changeReason, updatedBy,
                updatedAt);
    }

    @JsonProperty("ruleVersionStatus")
    public String ruleVersionStatus() {
        return versionStatus;
    }
}

package kr.ac.knue.commonfoundation.basic60;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate,
        String evaluationConfirmedYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
    @JsonProperty("ruleVersionStatus")
    public String ruleVersionStatus() {
        return versionStatus;
    }
}

package kr.ac.knue.commonfoundation.basic59;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParticipationRateOperationSettingRow(
        Long settingId,
        Long ruleVersionId,
        String versionCode,
        String versionStatus,
        String evaluationYear,
        String achievementAreaCode,
        String achievementCategoryCode,
        String managementItemCode,
        String researcherCountBand,
        String participationTypeCode,
        BigDecimal distributionRate,
        String activeYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}

package kr.ac.knue.commonfoundation.basic59;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ManagementItemEvaluationScoreRow(
        Long settingId,
        Long ruleVersionId,
        String versionCode,
        String versionStatus,
        String evaluationYear,
        String achievementAreaCode,
        String achievementCategoryCode,
        String managementItemCode,
        String collegeCode,
        BigDecimal evaluationScore,
        Integer sortOrder,
        String activeYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}

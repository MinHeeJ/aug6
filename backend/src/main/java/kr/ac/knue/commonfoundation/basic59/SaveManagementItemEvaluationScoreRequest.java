package kr.ac.knue.commonfoundation.basic59;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SaveManagementItemEvaluationScoreRequest(
        @NotNull Long ruleVersionId,
        @NotBlank String achievementAreaCode,
        @NotBlank String achievementCategoryCode,
        @NotBlank String managementItemCode,
        @NotBlank String collegeCode,
        @NotNull BigDecimal evaluationScore,
        @NotNull Integer sortOrder,
        @NotBlank String activeYn,
        @NotBlank String changeReason) {
}
